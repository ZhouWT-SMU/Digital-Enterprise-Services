const moduleTitle = document.getElementById('moduleTitle');
const moduleDescription = document.getElementById('moduleDescription');

const API_BASE_URL = '/api/chat';
const STORAGE_KEYS = {
    userId: 'enterpriseChatUserId',
    conversationPrefix: 'enterpriseChatConversation:',
    historyPrefix: 'enterpriseChatHistory:',
    historyActivePrefix: 'enterpriseChatActive:',
    historyCounterPrefix: 'enterpriseChatHistoryCounter:',
};

const DEFAULT_ASSISTANT_GREETING = '您好！我是企业智能助手，可以为您提供政策解读、项目申报、资源对接等服务。';
const DEFAULT_CONVERSATION_SUBTITLE = '智能助手已准备就绪';
const CONVERSATION_TITLE_PATTERN = /^对话\s\d+$/;
const STREAM_CONTENT_TYPE = 'application/x-ndjson';

const conversationStores = new Map();
const panelStates = new WeakMap();
let cachedUserId = null;

if (window.marked) {
    window.marked.setOptions({
        breaks: true,
        gfm: true,
    });
}

function getStorage() {
    try {
        return window.localStorage;
    } catch (error) {
        console.warn('本地存储不可用，使用临时会话。', error);
        return null;
    }
}

const storage = getStorage();

function safeParse(value, fallback) {
    if (!value) {
        return fallback;
    }

    try {
        return JSON.parse(value);
    } catch (error) {
        console.warn('无法解析会话存储内容', error);
        return fallback;
    }
}

function normaliseWhitespace(value) {
    return (value || '').replace(/\s+/g, ' ').trim();
}

function createSnippet(text, maxLength = 36) {
    const cleaned = normaliseWhitespace(text);
    if (!cleaned) {
        return '';
    }

    return cleaned.length > maxLength ? `${cleaned.slice(0, maxLength)}…` : cleaned;
}

function refreshConversationPreview(conversation) {
    if (!conversation) {
        return;
    }

    const messages = Array.isArray(conversation.messages) ? conversation.messages : [];
    const lastMessage = messages.length > 0 ? messages[messages.length - 1] : null;

    if (lastMessage && lastMessage.content) {
        const subtitle = createSnippet(lastMessage.content, 36);
        if (subtitle) {
            conversation.subtitle = subtitle;
        }

        const titleCandidate = createSnippet(lastMessage.content, 12);
        if (titleCandidate) {
            if (lastMessage.role === 'user' && (!conversation.title || CONVERSATION_TITLE_PATTERN.test(conversation.title))) {
                conversation.title = titleCandidate;
            } else if (!conversation.title || !conversation.title.trim()) {
                conversation.title = titleCandidate;
            }
        }
    }

    if (!conversation.subtitle) {
        conversation.subtitle = DEFAULT_CONVERSATION_SUBTITLE;
    }
}

function createConversation(historyKey, counter) {
    const now = Date.now();
    const conversation = {
        id: `${historyKey || 'conversation'}-${now}-${Math.random().toString(16).slice(2, 10)}`,
        title: `对话 ${counter}`,
        subtitle: createSnippet(DEFAULT_ASSISTANT_GREETING, 36) || DEFAULT_CONVERSATION_SUBTITLE,
        createdAt: now,
        updatedAt: now,
        conversationId: null,
        messages: [
            {
                role: 'assistant',
                content: DEFAULT_ASSISTANT_GREETING,
                createdAt: now,
            },
        ],
    };

    refreshConversationPreview(conversation);
    return conversation;
}

function getConversationStore(historyKey) {
    if (conversationStores.has(historyKey)) {
        return conversationStores.get(historyKey);
    }

    const store = {
        conversations: [],
        activeId: null,
        counter: 1,
    };

    if (storage) {
        const storedConversations = safeParse(
            storage.getItem(`${STORAGE_KEYS.historyPrefix}${historyKey}`),
            [],
        );

        if (Array.isArray(storedConversations)) {
            store.conversations = storedConversations.map((conversation) => {
                const normalised = { ...conversation };
                normalised.messages = Array.isArray(normalised.messages) ? normalised.messages : [];
                refreshConversationPreview(normalised);
                return normalised;
            });
        }

        const storedActive = storage.getItem(`${STORAGE_KEYS.historyActivePrefix}${historyKey}`);
        if (storedActive) {
            store.activeId = storedActive;
        }

        const storedCounter = parseInt(
            storage.getItem(`${STORAGE_KEYS.historyCounterPrefix}${historyKey}`),
            10,
        );
        if (!Number.isNaN(storedCounter) && storedCounter > 0) {
            store.counter = storedCounter;
        } else if (store.conversations.length > 0) {
            store.counter = store.conversations.length + 1;
        }
    }

    if (store.conversations.length === 0) {
        const conversation = createConversation(historyKey, store.counter);
        store.conversations = [conversation];
        store.activeId = conversation.id;
        store.counter += 1;
    } else if (!store.activeId || !store.conversations.some((item) => item.id === store.activeId)) {
        store.activeId = store.conversations[0].id;
    }

    conversationStores.set(historyKey, store);
    return store;
}

function saveConversationStore(historyKey, store) {
    if (!historyKey || !store) {
        return;
    }

    conversationStores.set(historyKey, store);

    if (!storage) {
        return;
    }

    try {
        storage.setItem(
            `${STORAGE_KEYS.historyPrefix}${historyKey}`,
            JSON.stringify(store.conversations),
        );
    } catch (error) {
        console.warn('无法保存对话历史', error);
    }

    try {
        if (store.activeId) {
            storage.setItem(`${STORAGE_KEYS.historyActivePrefix}${historyKey}`, store.activeId);
        } else {
            storage.removeItem(`${STORAGE_KEYS.historyActivePrefix}${historyKey}`);
        }
    } catch (error) {
        console.warn('无法保存当前会话标识', error);
    }

    try {
        storage.setItem(
            `${STORAGE_KEYS.historyCounterPrefix}${historyKey}`,
            String(store.counter || 1),
        );
    } catch (error) {
        console.warn('无法保存会话计数器', error);
    }
}

function sortConversations(store) {
    if (!store) {
        return;
    }

    store.conversations.sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0));
}

function getActiveConversation(store) {
    if (!store || !store.activeId) {
        return null;
    }

    return store.conversations.find((conversation) => conversation.id === store.activeId) || null;
}

function getUserId() {
    if (cachedUserId) {
        return cachedUserId;
    }

    if (storage) {
        const existing = storage.getItem(STORAGE_KEYS.userId);
        if (existing) {
            cachedUserId = existing;
            return cachedUserId;
        }
    }

    const generated = `web-${Date.now()}-${Math.random().toString(16).slice(2, 10)}`;
    if (storage) {
        storage.setItem(STORAGE_KEYS.userId, generated);
    }
    cachedUserId = generated;
    return cachedUserId;
}

function getPanelState(panel) {
    let state = panelStates.get(panel);
    if (state) {
        return state;
    }

    const key = panel.dataset.conversationKey || null;
    let conversationId = null;

    if (key && storage) {
        conversationId = storage.getItem(`${STORAGE_KEYS.conversationPrefix}${key}`);
    }

    const historyKey = panel.dataset.historyKey || key || null;
    const historyEnabled = panel.dataset.historyEnabled === 'true';

    state = {
        conversationKey: key,
        conversationId: conversationId || null,
        isLoading: false,
        historyEnabled,
        historyKey,
        historyStore: null,
        historyContainer: null,
        historyList: null,
        historyBody: null,
        activeConversationId: null,
    };

    panelStates.set(panel, state);
    return state;
}

function persistConversationId(state, conversationId) {
    if (!state) {
        return;
    }

    state.conversationId = conversationId;

    if (state.conversationKey && storage && conversationId) {
        storage.setItem(`${STORAGE_KEYS.conversationPrefix}${state.conversationKey}`, conversationId);
    }
}

function handleNavigation() {
    const menuItems = document.querySelectorAll('.menu-item');
    const modules = document.querySelectorAll('.module');

    menuItems.forEach((item) => {
        item.addEventListener('click', () => {
            if (item.classList.contains('active')) {
                return;
            }

            menuItems.forEach((button) => button.classList.remove('active'));
            modules.forEach((section) => section.classList.remove('active'));

            item.classList.add('active');
            const targetId = item.dataset.target;
            const targetModule = document.getElementById(targetId);

            if (targetModule) {
                targetModule.classList.add('active');
            }

            if (moduleTitle) {
                moduleTitle.textContent = item.dataset.title || '';
            }

            if (moduleDescription) {
                moduleDescription.textContent = item.dataset.description || '';
            }
        });
    });
}

function renderAssistantMessage(target, content) {
    if (!target) {
        return;
    }

    if (window.marked && typeof window.marked.parse === 'function') {
        target.innerHTML = window.marked.parse(content || '');
    } else {
        target.textContent = content;
    }
}

function appendMessage(history, role, content, labels, options = {}) {
    if (!history) {
        return null;
    }

    const message = document.createElement('div');
    message.className = `message ${role}`;

    if (options.pending) {
        message.classList.add('pending');
    }

    if (options.id) {
        message.dataset.messageId = options.id;
    }

    const meta = document.createElement('div');
    meta.className = 'message-meta';
    meta.textContent = role === 'user' ? labels.user : labels.assistant;

    const body = document.createElement('div');
    body.className = 'message-body';

    const shouldRenderMarkdown = role === 'assistant'
        && options.pending !== true
        && options.renderMarkdown !== false;

    if (shouldRenderMarkdown) {
        renderAssistantMessage(body, content);
    } else {
        body.textContent = content;
    }

    message.append(meta, body);
    history.appendChild(message);
    history.scrollTop = history.scrollHeight;

    return { element: message, body };
}

function deleteRemoteConversation(conversationId) {
    if (!conversationId) {
        return;
    }

    const params = new URLSearchParams({ userId: getUserId() });
    fetch(`${API_BASE_URL}/conversations/${encodeURIComponent(conversationId)}?${params.toString()}`, {
        method: 'DELETE',
    }).catch((error) => {
        console.warn('删除服务器会话失败', error);
    });
}

function refreshConversationList(panelState) {
    if (!panelState || !panelState.historyStore || !panelState.historyList) {
        return;
    }

    sortConversations(panelState.historyStore);

    const activeId = panelState.historyStore.activeId;
    panelState.historyList.innerHTML = '';

    panelState.historyStore.conversations.forEach((conversation) => {
        const item = document.createElement('li');
        item.className = 'conversation-item';
        item.dataset.conversationId = conversation.id;

        if (conversation.id === activeId) {
            item.classList.add('active');
        }

        const selectButton = document.createElement('button');
        selectButton.type = 'button';
        selectButton.className = 'conversation-button';

        const titleSpan = document.createElement('span');
        titleSpan.className = 'conversation-title';
        titleSpan.textContent = conversation.title || '未命名对话';

        const snippetSpan = document.createElement('span');
        snippetSpan.className = 'conversation-snippet';
        snippetSpan.textContent = conversation.subtitle || DEFAULT_CONVERSATION_SUBTITLE;

        selectButton.append(titleSpan, snippetSpan);

        const deleteButton = document.createElement('button');
        deleteButton.type = 'button';
        deleteButton.className = 'conversation-delete';
        deleteButton.dataset.conversationDelete = 'true';
        deleteButton.setAttribute('aria-label', '删除对话');
        deleteButton.textContent = '×';

        item.append(selectButton, deleteButton);
        panelState.historyList.appendChild(item);
    });

    if (panelState.historyBody) {
        if (panelState.historyStore.conversations.length === 0) {
            panelState.historyBody.classList.add('is-empty');
        } else {
            panelState.historyBody.classList.remove('is-empty');
        }
    }
}

function renderConversationMessages(panelState, historyElement, labels) {
    if (!panelState || !panelState.historyStore || !historyElement) {
        return;
    }

    const conversation = getActiveConversation(panelState.historyStore);
    historyElement.innerHTML = '';

    if (!conversation) {
        panelState.conversationId = null;
        panelState.activeConversationId = null;
        return;
    }

    conversation.messages.forEach((message) => {
        appendMessage(historyElement, message.role, message.content, labels, {
            renderMarkdown: message.role === 'assistant',
        });
    });

    historyElement.scrollTop = historyElement.scrollHeight;
    panelState.activeConversationId = conversation.id;
    panelState.conversationId = conversation.conversationId || null;
}

function activateConversation(panelState, conversationId, historyElement, labels) {
    if (!panelState || !panelState.historyStore || !conversationId) {
        return;
    }

    if (!panelState.historyStore.conversations.some((conversation) => conversation.id === conversationId)) {
        return;
    }

    if (panelState.historyStore.activeId === conversationId) {
        return;
    }

    panelState.historyStore.activeId = conversationId;
    saveConversationStore(panelState.historyKey, panelState.historyStore);
    refreshConversationList(panelState);
    renderConversationMessages(panelState, historyElement, labels);
}

function handleConversationDeletion(panelState, conversationId, labels, historyElement) {
    if (!panelState || !panelState.historyStore) {
        return;
    }

    const store = panelState.historyStore;
    const index = store.conversations.findIndex((conversation) => conversation.id === conversationId);
    if (index === -1) {
        return;
    }

    const [removed] = store.conversations.splice(index, 1);
    if (removed && removed.conversationId) {
        deleteRemoteConversation(removed.conversationId);
    }

    if (store.conversations.length === 0) {
        const fallback = createConversation(panelState.historyKey, store.counter);
        store.counter += 1;
        store.conversations.push(fallback);
    }

    if (!store.conversations.some((conversation) => conversation.id === store.activeId)) {
        store.activeId = store.conversations[0].id;
    }

    saveConversationStore(panelState.historyKey, store);
    refreshConversationList(panelState);
    renderConversationMessages(panelState, historyElement, labels);
}

function setupConversationHistory(panel, panelState, historyElement, input, labels) {
    if (!panelState.historyEnabled || !panelState.historyKey) {
        return;
    }

    const store = getConversationStore(panelState.historyKey);
    panelState.historyStore = store;

    const container = document.querySelector(
        `[data-conversation-panel][data-history-key="${panelState.historyKey}"]`,
    );

    if (container) {
        panelState.historyContainer = container;
        panelState.historyList = container.querySelector('[data-conversation-list]');
        panelState.historyBody = container.querySelector('.conversation-body');

        const newButton = container.querySelector('[data-conversation-new]');
        if (newButton) {
            newButton.addEventListener('click', () => {
                const nextConversation = createConversation(panelState.historyKey, store.counter);
                store.counter += 1;
                store.conversations.unshift(nextConversation);
                store.activeId = nextConversation.id;
                panelState.conversationId = null;
                panelState.activeConversationId = nextConversation.id;
                saveConversationStore(panelState.historyKey, store);
                refreshConversationList(panelState);
                renderConversationMessages(panelState, historyElement, labels);
                if (input) {
                    input.focus();
                }
            });
        }

        if (panelState.historyList) {
            panelState.historyList.addEventListener('click', (event) => {
                const deleteButton = event.target.closest('[data-conversation-delete]');
                const item = event.target.closest('.conversation-item');

                if (!item) {
                    return;
                }

                const conversationId = item.dataset.conversationId;
                if (!conversationId) {
                    return;
                }

                if (deleteButton) {
                    event.preventDefault();
                    event.stopPropagation();
                    handleConversationDeletion(panelState, conversationId, labels, historyElement);
                    return;
                }

                activateConversation(panelState, conversationId, historyElement, labels);
            });
        }
    }

    refreshConversationList(panelState);
    renderConversationMessages(panelState, historyElement, labels);
    saveConversationStore(panelState.historyKey, store);
}

function setupChatPanels() {
    const panels = document.querySelectorAll('[data-chat]');

    panels.forEach((panel) => {
        const history = panel.querySelector('[data-chat-history]');
        const input = panel.querySelector('[data-chat-input]');
        const sendButton = panel.querySelector('[data-chat-send]');

        if (!history || !input || !sendButton) {
            return;
        }

        const labels = {
            user: panel.dataset.userLabel || '我',
            assistant: panel.dataset.assistantLabel || '智能助手',
        };

        const replyTemplate = panel.dataset.replyTemplate
            || '已记录您的需求“{message}”。我们将结合企业画像为您准备相应的服务建议。';

        const panelState = getPanelState(panel);
        const defaultSendLabel = sendButton.textContent;

        if (panelState.historyEnabled) {
            setupConversationHistory(panel, panelState, history, input, labels);
        }

        const sendCurrentMessage = async () => {
            const text = input.value.trim();
            if (!text || panelState.isLoading) {
                return;
            }

            const isHistoryEnabled = panelState.historyEnabled;
            const historyKey = panelState.historyKey;
            const userId = getUserId();
            const timestamp = Date.now();

            let conversationRecord = null;
            let assistantRecord = null;

            if (isHistoryEnabled) {
                if (!panelState.historyStore) {
                    panelState.historyStore = getConversationStore(historyKey);
                }

                conversationRecord = getActiveConversation(panelState.historyStore);
                if (!conversationRecord) {
                    conversationRecord = createConversation(historyKey, panelState.historyStore.counter);
                    panelState.historyStore.counter += 1;
                    panelState.historyStore.conversations.unshift(conversationRecord);
                    panelState.historyStore.activeId = conversationRecord.id;
                }

                const userEntry = {
                    role: 'user',
                    content: text,
                    createdAt: timestamp,
                };
                conversationRecord.messages.push(userEntry);
                conversationRecord.updatedAt = timestamp;
                refreshConversationPreview(conversationRecord);
                panelState.conversationId = conversationRecord.conversationId || null;
                panelState.activeConversationId = conversationRecord.id;
                saveConversationStore(historyKey, panelState.historyStore);
                refreshConversationList(panelState);
            }

            appendMessage(history, 'user', text, labels);
            input.value = '';
            input.focus();

            panelState.isLoading = true;
            sendButton.disabled = true;
            sendButton.textContent = '发送中…';

            const pending = appendMessage(history, 'assistant', '正在生成回复…', labels, { pending: true });

            if (isHistoryEnabled && conversationRecord) {
                assistantRecord = {
                    role: 'assistant',
                    content: '',
                    createdAt: Date.now(),
                };
                conversationRecord.messages.push(assistantRecord);
            }

            const payload = {
                message: text,
                userId,
            };

            if (panelState.conversationId) {
                payload.conversationId = panelState.conversationId;
            }

            let assistantContent = '';
            let finalConversationId = panelState.conversationId || null;
            let finalMessageId = null;
            let streamError = null;
            let reader;

            const applyAssistantContent = (content) => {
                assistantContent = content;
                renderAssistantMessage(pending.body, assistantContent);
                if (assistantRecord) {
                    assistantRecord.content = assistantContent;
                }
            };

            try {
                const response = await fetch(`${API_BASE_URL}/send-stream`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        Accept: STREAM_CONTENT_TYPE,
                    },
                    body: JSON.stringify(payload),
                });

                if (!response.ok) {
                    let errorMessage = `请求失败，状态码 ${response.status}`;
                    try {
                        const errorText = await response.text();
                        if (errorText) {
                            const parsed = JSON.parse(errorText);
                            if (parsed && parsed.error) {
                                errorMessage = parsed.error;
                            }
                        }
                    } catch (parseError) {
                        // 忽略解析异常
                    }
                    throw new Error(errorMessage);
                }

                if (!response.body) {
                    throw new Error('无法读取服务器响应。');
                }

                reader = response.body.getReader();
                const decoder = new TextDecoder();
                let buffer = '';

                const processLine = (line) => {
                    if (!line) {
                        return;
                    }

                    let payloadChunk;
                    try {
                        payloadChunk = JSON.parse(line);
                    } catch (parseError) {
                        console.warn('忽略无法解析的流式片段', line, parseError);
                        return;
                    }

                    if (payloadChunk.type === 'chunk') {
                        const delta = payloadChunk.delta || '';
                        if (delta) {
                            applyAssistantContent(assistantContent + delta);
                        }
                    } else if (payloadChunk.type === 'result') {
                        finalConversationId = payloadChunk.conversationId || finalConversationId;
                        finalMessageId = payloadChunk.messageId || finalMessageId;
                        if (typeof payloadChunk.answer === 'string' && !assistantContent) {
                            applyAssistantContent(payloadChunk.answer);
                        }
                    } else if (payloadChunk.type === 'error') {
                        const errorMessage = payloadChunk.error || '对话失败';
                        streamError = new Error(errorMessage);
                    }
                };

                while (true) {
                    const { value, done } = await reader.read();
                    if (done) {
                        break;
                    }

                    buffer += decoder.decode(value, { stream: true });

                    let newlineIndex = buffer.indexOf('\n');
                    while (newlineIndex !== -1) {
                        const line = buffer.slice(0, newlineIndex).trim();
                        buffer = buffer.slice(newlineIndex + 1);
                        processLine(line);
                        if (streamError) {
                            break;
                        }
                        newlineIndex = buffer.indexOf('\n');
                    }

                    if (streamError) {
                        break;
                    }
                }

                if (!streamError && buffer.trim()) {
                    processLine(buffer.trim());
                }

                if (streamError) {
                    throw streamError;
                }

                pending.element.classList.remove('pending');
                if (!assistantContent) {
                    applyAssistantContent('对话已完成。');
                }

                if (finalMessageId) {
                    pending.element.dataset.messageId = finalMessageId;
                }

                if (isHistoryEnabled && conversationRecord) {
                    conversationRecord.conversationId = finalConversationId || conversationRecord.conversationId || null;
                    conversationRecord.updatedAt = Date.now();
                    refreshConversationPreview(conversationRecord);
                    saveConversationStore(historyKey, panelState.historyStore);
                    refreshConversationList(panelState);
                } else if (finalConversationId) {
                    persistConversationId(panelState, finalConversationId);
                }

                if (finalConversationId) {
                    panelState.conversationId = finalConversationId;
                }
            } catch (error) {
                console.error('调用智能助手失败:', error);
                pending.element.classList.remove('pending');
                pending.element.classList.add('error');

                const fallback = assistantContent
                    || (replyTemplate
                        ? replyTemplate.replace('{message}', text)
                        : `抱歉，暂时无法获取智能助手回复：${error.message}`);

                if (assistantContent) {
                    renderAssistantMessage(pending.body, fallback);
                } else {
                    pending.body.textContent = fallback;
                }

                if (assistantRecord) {
                    assistantRecord.content = fallback;
                }

                if (panelState.historyEnabled && panelState.historyStore && conversationRecord) {
                    conversationRecord.updatedAt = Date.now();
                    refreshConversationPreview(conversationRecord);
                    saveConversationStore(historyKey, panelState.historyStore);
                    refreshConversationList(panelState);
                }
            } finally {
                if (reader) {
                    try {
                        reader.releaseLock();
                    } catch (releaseError) {
                        // 忽略释放异常
                    }
                }
                panelState.isLoading = false;
                sendButton.disabled = false;
                sendButton.textContent = defaultSendLabel;
                history.scrollTop = history.scrollHeight;
            }
        };

        sendButton.addEventListener('click', sendCurrentMessage);

        input.addEventListener('keydown', (event) => {
            if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault();
                sendCurrentMessage();
            }
        });

        const suggestions = panel.querySelectorAll('[data-chat-suggestion]');
        suggestions.forEach((button) => {
            button.addEventListener('click', () => {
                input.value = button.dataset.text || '';
                input.focus();
            });
        });
    });
}

function createDynamicItem(container) {
    const item = document.createElement('div');
    item.className = 'dynamic-item';

    const input = document.createElement('input');
    input.type = 'text';
    input.name = container.dataset.name;
    input.placeholder = container.dataset.placeholder || '';

    if (container.dataset.required === 'true') {
        input.required = true;
    }

    const removeButton = document.createElement('button');
    removeButton.type = 'button';
    removeButton.className = 'remove-item';
    removeButton.setAttribute('aria-label', '删除');
    removeButton.textContent = '×';

    item.append(input, removeButton);
    return item;
}

function updateRemoveButtons(container) {
    const items = container.querySelectorAll('.dynamic-item');
    const shouldShow = items.length > 1;
    items.forEach((item) => {
        const removeButton = item.querySelector('.remove-item');
        if (removeButton) {
            removeButton.style.visibility = shouldShow ? 'visible' : 'hidden';
        }
    });
}

function setupDynamicList(containerId) {
    const container = document.getElementById(containerId);
    const addButton = document.querySelector(`.add-item[data-target="${containerId}"]`);

    if (!container || !addButton) {
        return;
    }

    if (container.dataset.required === 'true') {
        const firstInput = container.querySelector('input');
        if (firstInput) {
            firstInput.required = true;
        }
    }

    updateRemoveButtons(container);

    addButton.addEventListener('click', () => {
        const newItem = createDynamicItem(container);
        container.appendChild(newItem);
        const newInput = newItem.querySelector('input');
        if (newInput) {
            newInput.focus();
        }
        updateRemoveButtons(container);
    });

    container.addEventListener('click', (event) => {
        if (event.target.classList.contains('remove-item')) {
            const item = event.target.closest('.dynamic-item');
            if (item) {
                item.remove();
                updateRemoveButtons(container);
            }
        }
    });
}

function normaliseFormData(formData) {
    const result = {};

    formData.forEach((value, key) => {
        const normalisedKey = key.endsWith('[]') ? key.slice(0, -2) : key;
        const trimmedValue = value.trim();
        if (!trimmedValue) {
            return;
        }

        if (Object.prototype.hasOwnProperty.call(result, normalisedKey)) {
            if (!Array.isArray(result[normalisedKey])) {
                result[normalisedKey] = [result[normalisedKey]];
            }
            result[normalisedKey].push(trimmedValue);
        } else {
            result[normalisedKey] = trimmedValue;
        }
    });

    return result;
}

function renderArraySection(title, values) {
    if (!values || values.length === 0) {
        return '';
    }

    const listItems = values
        .map((entry) => `<li>${entry}</li>`)
        .join('');

    return `
        <div class="result-section">
            <h4>${title}</h4>
            <ul>${listItems}</ul>
        </div>
    `;
}

function setupCapabilityForm() {
    const form = document.getElementById('capabilityForm');
    const resultContainer = document.getElementById('formResult');

    if (!form || !resultContainer) {
        return;
    }

    ['coreProductsList', 'intellectualPropertiesList', 'patentList'].forEach((id) => {
        setupDynamicList(id);
    });

    form.addEventListener('submit', (event) => {
        event.preventDefault();
        const formData = new FormData(form);
        const normalised = normaliseFormData(formData);

        const { companyName, creditCode, companyScale, companyAddress, companyType, businessIntro, contactName, contactInfo } = normalised;

        const summary = `
            <div class="result-summary">
                <h3>能力信息提交成功</h3>
                <p>感谢您提交 <strong>${companyName || '未命名企业'}</strong> 的最新能力信息，我们的顾问将尽快与您联系。</p>
                <div class="result-grid">
                    <div><span>统一信用代码</span><strong>${creditCode || '-'}</strong></div>
                    <div><span>企业规模</span><strong>${companyScale || '-'}</strong></div>
                    <div><span>企业类型</span><strong>${companyType || '-'}</strong></div>
                    <div><span>企业地址</span><strong>${companyAddress || '-'}</strong></div>
                    <div><span>联系人</span><strong>${contactName || '-'}</strong></div>
                    <div><span>联系方式</span><strong>${contactInfo || '-'}</strong></div>
                </div>
                <div class="result-section">
                    <h4>业务简介</h4>
                    <p>${businessIntro || '—'}</p>
                </div>
                ${renderArraySection('核心产品', Array.isArray(normalised.coreProducts) ? normalised.coreProducts : normalised.coreProducts ? [normalised.coreProducts] : [])}
                ${renderArraySection('知识产权', Array.isArray(normalised.intellectualProperties) ? normalised.intellectualProperties : normalised.intellectualProperties ? [normalised.intellectualProperties] : [])}
                ${renderArraySection('专利', Array.isArray(normalised.patents) ? normalised.patents : normalised.patents ? [normalised.patents] : [])}
            </div>
        `;

        resultContainer.innerHTML = summary;
        resultContainer.classList.add('visible');

        form.reset();
        ['coreProductsList', 'intellectualPropertiesList', 'patentList'].forEach((id) => resetDynamicList(id));
    });

    form.addEventListener('reset', () => {
        setTimeout(() => {
            resultContainer.innerHTML = '';
            resultContainer.classList.remove('visible');
            ['coreProductsList', 'intellectualPropertiesList', 'patentList'].forEach((id) => resetDynamicList(id));
        }, 0);
    });
}

function resetDynamicList(containerId) {
    const container = document.getElementById(containerId);
    if (!container) {
        return;
    }

    const items = container.querySelectorAll('.dynamic-item');
    items.forEach((item, index) => {
        const input = item.querySelector('input');
        if (index === 0) {
            if (input) {
                input.value = '';
            }
        } else {
            item.remove();
        }
    });

    updateRemoveButtons(container);
}

document.addEventListener('DOMContentLoaded', () => {
    handleNavigation();
    setupChatPanels();
    setupCapabilityForm();
});
