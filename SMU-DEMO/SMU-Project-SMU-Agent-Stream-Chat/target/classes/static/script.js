// 全局变量
let currentConversationId = null;
let conversations = [];
let messages = [];
let pendingConfirmAction = null; // 待执行的确认操作

// 确认操作类型
const CONFIRM_TYPES = {
    DELETE_CONVERSATION: 'delete_conversation',
    CLEAR_CHAT: 'clear_chat',
    EXPORT_CHAT: 'export_chat'
};

// API 配置
const API_BASE_URL = 'http://localhost:8080/api/chat';

// 初始化
document.addEventListener('DOMContentLoaded', function () {
    loadConversations();
    setupEventListeners();

    // 点击模态对话框外部关闭
    const modal = document.getElementById('confirmModal');
    modal.addEventListener('click', function (event) {
        if (event.target === modal) {
            closeConfirmModal();
        }
    });
});

// 设置事件监听器
function setupEventListeners() {
    const messageInput = document.getElementById('messageInput');
    messageInput.addEventListener('input', function () {
        this.style.height = 'auto';
        this.style.height = Math.min(this.scrollHeight, 120) + 'px';
    });
}

// 加载会话列表
async function loadConversations() {
    try {
        const response = await fetch(`${API_BASE_URL}/conversations?userId=test-user&limit=20`);
        const data = await response.json();

        if (data.success) {
            conversations = data.data || [];
            renderConversations();
        }
    } catch (error) {
        console.error('加载会话失败:', error);
        showError('加载会话失败');
    }
}

// 渲染会话列表
function renderConversations() {
    const conversationList = document.getElementById('conversationList');
    conversationList.innerHTML = '';

    // 按时间倒序排序，最近的对话在最上面
    const sortedConversations = [...conversations].sort((a, b) => {
        const timeA = typeof a.updatedAt === 'number'
            ? (a.updatedAt > 1000000000000 ? a.updatedAt : a.updatedAt * 1000)
            : new Date(a.updatedAt).getTime();
        const timeB = typeof b.updatedAt === 'number'
            ? (b.updatedAt > 1000000000000 ? b.updatedAt : b.updatedAt * 1000)
            : new Date(b.updatedAt).getTime();
        return timeB - timeA; // 倒序：最新的在前
    });

    sortedConversations.forEach(conversation => {
        const conversationItem = document.createElement('div');
        conversationItem.className = 'conversation-item';

        const title = conversation.name || '未命名对话';
        // 处理时间戳 - 可能是毫秒或秒
        const timestamp = typeof conversation.updatedAt === 'number'
            ? (conversation.updatedAt > 1000000000000 ? conversation.updatedAt : conversation.updatedAt * 1000)
            : conversation.updatedAt;
        const time = new Date(timestamp).toLocaleString();

        conversationItem.innerHTML = `
            <div class="conversation-content" onclick="selectConversation('${conversation.id}')">
                <div class="conversation-title">${title}</div>
                <div class="conversation-time">${time}</div>
            </div>
            <div class="conversation-actions">
                <button class="delete-btn" onclick="deleteConversation('${conversation.id}', event)" title="删除会话">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
        `;

        conversationList.appendChild(conversationItem);
    });
}

// 选择会话
async function selectConversation(conversationId) {
    console.log('选择会话:', conversationId);
    currentConversationId = conversationId;

    // 更新UI - 移除所有active状态，然后为当前会话添加active状态
    document.querySelectorAll('.conversation-item').forEach(item => {
        item.classList.remove('active');
    });

    // 找到对应的会话项并添加active状态
    const conversationItems = document.querySelectorAll('.conversation-item');
    let found = false;
    for (let item of conversationItems) {
        const deleteBtn = item.querySelector('.delete-btn');
        if (deleteBtn && deleteBtn.onclick.toString().includes(conversationId)) {
            item.classList.add('active');
            found = true;
            break;
        }
    }

    if (!found) {
        console.warn('未找到对应的会话项:', conversationId);
    }

    // 更新会话标题
    const conversation = conversations.find(c => c.id === conversationId);
    if (conversation) {
        document.getElementById('currentConversationTitle').textContent = conversation.name || '未命名对话';
    }

    // 加载消息历史
    await loadMessageHistory(conversationId);
}

// 显示通用确认模态对话框
function showConfirmModal(type, data = {}) {
    pendingConfirmAction = { type, data };
    const modal = document.getElementById('confirmModal');
    const title = document.getElementById('modalTitle');
    const message = document.getElementById('modalMessage');
    const warning = document.getElementById('modalWarning');
    const confirmBtn = document.getElementById('confirmButton');

    // 根据操作类型设置不同的样式和内容
    switch (type) {
        case CONFIRM_TYPES.DELETE_CONVERSATION:
            title.innerHTML = '<i class="fas fa-exclamation-triangle"></i> 确认删除';
            title.className = 'danger';
            message.textContent = '确定要删除这个会话吗？';
            warning.style.display = 'block';
            warning.textContent = '删除后无法恢复，请谨慎操作。';
            confirmBtn.textContent = '删除';
            confirmBtn.className = 'btn-confirm danger';
            break;

        case CONFIRM_TYPES.CLEAR_CHAT:
            title.innerHTML = '<i class="fas fa-trash"></i> 确认清空';
            title.className = 'warning';
            message.textContent = '确定要清空当前对话吗？';
            warning.style.display = 'block';
            warning.textContent = '清空后无法恢复，请谨慎操作。';
            confirmBtn.textContent = '清空';
            confirmBtn.className = 'btn-confirm warning';
            break;

        case CONFIRM_TYPES.EXPORT_CHAT:
            title.innerHTML = '<i class="fas fa-download"></i> 确认导出';
            title.className = 'success';
            message.textContent = '确定要导出当前对话记录吗？';
            warning.style.display = 'none';
            confirmBtn.textContent = '导出';
            confirmBtn.className = 'btn-confirm success';
            break;

        default:
            title.innerHTML = '<i class="fas fa-question-circle"></i> 确认操作';
            title.className = '';
            message.textContent = '确定要执行此操作吗？';
            warning.style.display = 'none';
            confirmBtn.textContent = '确认';
            confirmBtn.className = 'btn-confirm';
    }

    modal.style.display = 'block';
}

// 关闭通用确认模态对话框
function closeConfirmModal() {
    const modal = document.getElementById('confirmModal');
    modal.style.display = 'none';
    pendingConfirmAction = null;
}

// 执行确认操作
async function executeConfirmAction() {
    if (!pendingConfirmAction) {
        return;
    }

    const { type, data } = pendingConfirmAction;
    closeConfirmModal();

    switch (type) {
        case CONFIRM_TYPES.DELETE_CONVERSATION:
            await deleteConversationAction(data.conversationId);
            break;

        case CONFIRM_TYPES.CLEAR_CHAT:
            clearChatAction();
            break;

        case CONFIRM_TYPES.EXPORT_CHAT:
            exportChatAction();
            break;
    }
}

// 删除会话操作
async function deleteConversationAction(conversationId) {
    try {
        const response = await fetch(`${API_BASE_URL}/conversations/${conversationId}?userId=test-user`, {
            method: 'DELETE'
        });

        const data = await response.json();

        if (data.success) {
            showSuccess('会话删除成功');
            await loadConversations();

            // 如果删除的是当前会话，清空聊天界面
            if (currentConversationId === conversationId) {
                createNewChat();
            }
        } else {
            showError(data.error || '删除会话失败');
        }
    } catch (error) {
        console.error('删除会话失败:', error);
        showError('删除会话失败');
    }
}

// 清空聊天操作
function clearChatAction() {
    messages = [];
    document.getElementById('chatMessages').innerHTML = '';
    showSuccess('聊天记录已清空');
}

// 导出聊天记录操作
function exportChatAction() {
    if (messages.length === 0) {
        showError('没有聊天记录可导出');
        return;
    }

    const chatData = {
        conversationId: currentConversationId,
        messages: messages,
        exportTime: new Date().toISOString()
    };

    const dataStr = JSON.stringify(chatData, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });

    const link = document.createElement('a');
    link.href = URL.createObjectURL(dataBlob);
    link.download = `chat-export-${Date.now()}.json`;
    link.click();

    showSuccess('聊天记录导出成功');
}

// 删除会话
function deleteConversation(conversationId, event) {
    event.stopPropagation();
    showConfirmModal(CONFIRM_TYPES.DELETE_CONVERSATION, { conversationId });
}

// 加载消息历史
async function loadMessageHistory(conversationId) {
    try {
        const response = await fetch(`${API_BASE_URL}/history/${conversationId}?userId=test-user&limit=50`);
        const data = await response.json();

        if (data.success) {
            // 转换后端消息格式为前端期望的格式
            messages = (data.data || []).map(message => {
                // 用户消息
                const userMessage = {
                    role: 'user',
                    content: message.query || '',
                    createdAt: message.createdAt * 1000 // 转换为毫秒
                };

                // AI回复消息
                const assistantMessage = {
                    role: 'assistant',
                    content: message.answer || '',
                    messageId: message.id,
                    createdAt: message.createdAt * 1000 // 转换为毫秒
                };

                return [userMessage, assistantMessage];
            }).flat().filter(msg => msg.content && msg.content.trim() !== ''); // 过滤空消息

            renderMessages();
        }
    } catch (error) {
        console.error('加载消息历史失败:', error);
        showError('加载消息历史失败');
    }
}

// 渲染消息
function renderMessages() {
    const chatMessages = document.getElementById('chatMessages');
    chatMessages.innerHTML = '';

    messages.forEach(message => {
        if (message.role === 'user') {
            const messageElement = createUserMessageElement(message.content, message.createdAt);
            chatMessages.appendChild(messageElement);
        } else if (message.role === 'assistant') {
            const messageElement = createAIMessageElementWithThink(message.content, message.createdAt);
            chatMessages.appendChild(messageElement);
        }
    });

    scrollToBottom();
}

// 创建用户消息元素
function createUserMessageElement(content, timestamp) {
    const messageDiv = document.createElement('div');
    messageDiv.className = 'message user';

    messageDiv.innerHTML = `
        <div class="message-content">
            <div class="message-text">${content}</div>
            <div class="message-time">${formatTime(timestamp)}</div>
        </div>
        <div class="message-avatar">
            <i class="fas fa-user"></i>
        </div>
    `;

    return messageDiv;
}

// 创建AI消息元素
function createAIMessageElement(content, timestamp) {
    const messageDiv = document.createElement('div');
    messageDiv.className = 'message assistant';

    messageDiv.innerHTML = `
        <div class="message-avatar">
            <i class="fas fa-robot"></i>
        </div>
        <div class="message-content">
            <div class="message-text">${content}</div>
            <div class="message-time">${formatTime(timestamp)}</div>
        </div>
    `;

    return messageDiv;
}

// 创建支持思考内容的AI消息元素
function createAIMessageElementWithThink(content, timestamp) {
    const messageDiv = document.createElement('div');
    messageDiv.className = 'message assistant';

    // 解析内容，分离think和正常回复
    const { thinkContent, normalContent } = parseContent(content);

    messageDiv.innerHTML = `
        <div class="message-avatar">
            <i class="fas fa-robot"></i>
        </div>
        <div class="message-content">
            ${thinkContent ? `
                <div class="think-section">
                    <div class="think-header">
                        <i class="fas fa-brain"></i> 思考过程
                    </div>
                    <div class="think-content">${thinkContent}</div>
                </div>
            ` : ''}
            <div class="message-text">${normalContent}</div>
            <div class="message-time">${formatTime(timestamp)}</div>
        </div>
    `;

    return messageDiv;
}

// 发送消息
async function sendMessage() {
    const messageInput = document.getElementById('messageInput');
    const message = messageInput.value.trim();

    if (!message) return;

    // 添加用户消息到界面
    addUserMessage(message);
    messageInput.value = '';
    messageInput.style.height = 'auto';

    // 显示加载状态
    showLoading();

    try {
        // 使用流式API
        const response = await fetch(`${API_BASE_URL}/send-stream`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                message: message,
                userId: 'test-user'
            })
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();

        if (data.success) {
            // 根据切换状态选择渲染方式
            const streaming = document.getElementById('streamingToggle')?.checked;

            if (streaming) {
                // 流式渲染
                addStreamingAssistantMessage(data.answer, data.messageId);
            } else {
                // 完整渲染
                addAssistantMessage(data.answer, data.messageId);
            }

            // 更新当前会话ID
            if (!currentConversationId) {
                currentConversationId = data.conversationId;
                await loadConversations();
            }
        } else {
            showError(data.error || '发送消息失败');
        }
    } catch (error) {
        console.error('发送消息失败:', error);
        showError('网络错误，请重试');
    } finally {
        hideLoading();
    }
}

// 添加用户消息
function addUserMessage(content) {
    const message = {
        role: 'user',
        content: content,
        createdAt: Date.now()
    };

    messages.push(message);

    const chatMessages = document.getElementById('chatMessages');
    const messageElement = createUserMessageElement(content, Date.now());
    chatMessages.appendChild(messageElement);

    scrollToBottom();
}

// 添加AI回复
function addAssistantMessage(content, messageId) {
    const message = {
        role: 'assistant',
        content: content,
        messageId: messageId,
        createdAt: Date.now()
    };

    messages.push(message);

    const chatMessages = document.getElementById('chatMessages');
    const messageElement = createAIMessageElementWithThink(content, Date.now());
    chatMessages.appendChild(messageElement);

    scrollToBottom();
}

// 添加流式AI回复
function addStreamingAssistantMessage(content, messageId) {
    const message = {
        role: 'assistant',
        content: content,
        messageId: messageId,
        createdAt: Date.now()
    };

    messages.push(message);

    const chatMessages = document.getElementById('chatMessages');
    const messageDiv = document.createElement('div');
    messageDiv.className = 'message assistant streaming';

    // 解析内容，分离think和正常回复
    const { thinkContent, normalContent } = parseContent(content);

    messageDiv.innerHTML = `
        <div class="message-avatar">
            <i class="fas fa-robot"></i>
        </div>
        <div class="message-content">
            ${thinkContent ? `
                <div class="think-section">
                    <div class="think-header">
                        <i class="fas fa-brain"></i> 思考过程
                    </div>
                    <div class="think-content"></div>
                </div>
            ` : ''}
            <div class="message-text streaming-text"></div>
            <div class="message-time">${formatTime(Date.now())}</div>
            <div class="streaming-indicator">
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
            </div>
        </div>
    `;
    chatMessages.appendChild(messageDiv);
    scrollToBottom();

    // 逐字渲染
    const textDiv = messageDiv.querySelector('.message-text');
    const thinkDiv = messageDiv.querySelector('.think-content');
    let i = 0;
    let thinkIndex = 0;

    function typeNext() {
        if (i <= normalContent.length) {
            textDiv.textContent = normalContent.slice(0, i);
            i++;
            scrollToBottom();
            setTimeout(typeNext, 15); // 打字速度可调
        } else {
            // 渲染完成后去掉流式动画
            messageDiv.classList.remove('streaming');
            const indicator = messageDiv.querySelector('.streaming-indicator');
            if (indicator) indicator.style.display = 'none';

            // 移除闪烁光标效果
            if (textDiv) {
                textDiv.classList.remove('streaming-text');
            }
        }
    }

    // 如果有思考内容，先渲染思考部分
    if (thinkContent && thinkDiv) {
        let thinkI = 0;
        function typeThink() {
            if (thinkI <= thinkContent.length) {
                thinkDiv.textContent = thinkContent.slice(0, thinkI);
                thinkI++;
                scrollToBottom();
                setTimeout(typeThink, 10); // 思考部分打字稍快
            } else {
                // 思考部分完成后开始渲染正常回复
                setTimeout(typeNext, 500); // 暂停500ms后开始回复
            }
        }
        typeThink();
    } else {
        typeNext();
    }
}

// 显示加载状态
function showLoading() {
    const chatMessages = document.getElementById('chatMessages');
    const loadingDiv = document.createElement('div');
    loadingDiv.className = 'message assistant';
    loadingDiv.id = 'loadingMessage';

    loadingDiv.innerHTML = `
        <div class="message-avatar">
            <i class="fas fa-robot"></i>
        </div>
        <div class="message-content">
            <div class="loading">
                <span>AI正在思考</span>
                <div class="loading-dots">
                    <span></span>
                    <span></span>
                    <span></span>
                </div>
            </div>
        </div>
    `;

    chatMessages.appendChild(loadingDiv);
    scrollToBottom();
}

// 隐藏加载状态
function hideLoading() {
    const loadingMessage = document.getElementById('loadingMessage');
    if (loadingMessage) {
        loadingMessage.remove();
    }
}

// 滚动到底部
function scrollToBottom() {
    const chatMessages = document.getElementById('chatMessages');
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

// 创建新对话
function createNewChat() {
    currentConversationId = null;
    messages = [];

    // 重置UI
    document.querySelectorAll('.conversation-item').forEach(item => {
        item.classList.remove('active');
    });

    document.getElementById('currentConversationTitle').textContent = '新对话';
    document.getElementById('chatMessages').innerHTML = '';
}

// 键盘事件处理
function handleKeyDown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
}

// 清空聊天
function clearChat() {
    showConfirmModal(CONFIRM_TYPES.CLEAR_CHAT);
}

// 导出聊天记录
function exportChat() {
    showConfirmModal(CONFIRM_TYPES.EXPORT_CHAT);
}

// 显示成功信息
function showSuccess(message) {
    console.log(message);
    showToast(message, 'success');
}

// 显示错误信息
function showError(message) {
    console.error(message);
    showToast(message, 'error');
}

// 显示Toast通知
function showToast(message, type = 'info') {
    // 创建toast元素
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
        <div class="toast-content">
            <i class="fas ${type === 'success' ? 'fa-check-circle' : type === 'error' ? 'fa-exclamation-circle' : 'fa-info-circle'}"></i>
            <span>${message}</span>
        </div>
    `;

    // 添加到页面
    document.body.appendChild(toast);

    // 显示动画
    setTimeout(() => {
        toast.classList.add('show');
    }, 100);

    // 自动隐藏
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 300);
    }, 3000);
}

// 解析内容，分离think和正常回复
function parseContent(content) {
    const thinkMatch = content.match(/<think>(.*?)<\/think>/s);
    let thinkContent = '';
    let normalContent = content;

    if (thinkMatch) {
        thinkContent = thinkMatch[1].trim();
        normalContent = content.replace(/<think>.*?<\/think>/s, '').trim();
    }

    return { thinkContent, normalContent };
}

// 格式化时间
function formatTime(timestamp) {
    const date = new Date(timestamp);
    return date.toLocaleString();
} 