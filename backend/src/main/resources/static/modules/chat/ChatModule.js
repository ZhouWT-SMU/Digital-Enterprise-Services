import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'https://unpkg.com/vue@3/dist/vue.esm-browser.prod.js';

const defaultFilters = () => ({
  q: '',
  industry: [],
  size: [],
  region: [],
  tech: []
});

export default {
  name: 'ChatModule',
  setup() {
    const filters = reactive(defaultFilters());
    const sizeOptions = ['小型', '中型', '大型'];
    const industries = ['制造业', '互联网', '金融', '能源'];
    const regions = ['华北', '华东', '华南', '华中', '西部'];
    const messageInput = ref('找在华东、有 AI 能力的中型制造企业');
    const tokens = ref('');
    const companies = ref([]);
    const loading = ref(false);
    const sessionId = ref(localStorage.getItem('dify-chat-session') || '');
    const eventSource = ref(null);
    const techText = ref('');

    const activeFilters = computed(() => ({
      q: filters.q,
      industry: filters.industry,
      size: filters.size,
      region: filters.region,
      tech: filters.tech
    }));

    function resetFilters() {
      Object.assign(filters, defaultFilters());
      techText.value = '';
    }

    function toggleSelection(list, value) {
      const index = list.indexOf(value);
      if (index === -1) {
        list.push(value);
      } else {
        list.splice(index, 1);
      }
    }

    function connectStream(payload) {
      const query = new URLSearchParams({ message: payload.message });
      if (payload.sessionId) {
        query.set('sessionId', payload.sessionId);
      }
      if (payload.filters) {
        query.set('filters', JSON.stringify(payload.filters));
      }
      const url = `/api/chat/stream?${query.toString()}`;
      if (eventSource.value) {
        eventSource.value.close();
      }
      const source = new EventSource(url);
      eventSource.value = source;
      loading.value = true;
      tokens.value = '';
      companies.value = [];

      source.addEventListener('token', (event) => {
        tokens.value += event.data;
      });

      source.addEventListener('companies', (event) => {
        try {
          companies.value = JSON.parse(event.data);
        } catch (err) {
          console.error('Failed to parse companies', err);
        }
      });

      source.addEventListener('session', (event) => {
        sessionId.value = event.data;
        localStorage.setItem('dify-chat-session', event.data);
      });

      source.addEventListener('done', () => {
        loading.value = false;
        source.close();
        eventSource.value = null;
      });

      source.addEventListener('error', (event) => {
        console.warn('Stream error', event.data);
        loading.value = false;
        source.close();
        eventSource.value = null;
      });
    }

    function submitQuery() {
      onTechChange();
      connectStream({
        message: messageInput.value,
        sessionId: sessionId.value,
        filters: activeFilters.value
      });
    }

    function onTechChange() {
      filters.tech = techText.value.split(',').map((s) => s.trim()).filter(Boolean);
    }

    function clearSession() {
      localStorage.removeItem('dify-chat-session');
      sessionId.value = '';
      sessionStorage.clear();
    }

    onMounted(() => {
      if (!sessionId.value) {
        localStorage.removeItem('dify-chat-session');
      }
    });

    onBeforeUnmount(() => {
      if (eventSource.value) {
        eventSource.value.close();
        eventSource.value = null;
      }
    });

    return {
      filters,
      industries,
      sizeOptions,
      regions,
      messageInput,
      tokens,
      companies,
      loading,
      sessionId,
      toggleSelection,
      resetFilters,
      submitQuery,
      onTechChange,
      techText,
      clearSession
    };
  },
  template: `
    <section class="chat-layout">
      <aside class="filter-panel" aria-label="筛选器">
        <div class="filter-group">
          <label for="query">关键词</label>
          <textarea id="query" v-model="filters.q" rows="3" placeholder="例如：智能制造、工业互联网"></textarea>
        </div>
        <div class="filter-group">
          <label>行业</label>
          <div class="company-tags">
            <button v-for="item in industries" :key="item" type="button" class="button secondary" :class="{ active: filters.industry.includes(item) }" @click="toggleSelection(filters.industry, item)">
              {{ item }}
            </button>
          </div>
        </div>
        <div class="filter-group">
          <label>规模</label>
          <div class="company-tags">
            <button v-for="item in sizeOptions" :key="item" type="button" class="button secondary" :class="{ active: filters.size.includes(item) }" @click="toggleSelection(filters.size, item)">
              {{ item }}
            </button>
          </div>
        </div>
        <div class="filter-group">
          <label>地区</label>
          <div class="company-tags">
            <button v-for="item in regions" :key="item" type="button" class="button secondary" :class="{ active: filters.region.includes(item) }" @click="toggleSelection(filters.region, item)">
              {{ item }}
            </button>
          </div>
        </div>
        <div class="filter-group">
          <label for="tech">技术关键词</label>
          <input id="tech" type="text" v-model="techText" placeholder="用逗号分隔多个关键词" @change="onTechChange" />
        </div>
        <div class="filter-actions">
          <button class="button" type="button" @click="submitQuery" :disabled="loading">开始对话</button>
          <button class="button secondary" type="button" @click="resetFilters">重置</button>
        </div>
        <div class="filter-actions">
          <button class="button secondary" type="button" @click="clearSession">清空会话</button>
        </div>
      </aside>
      <section class="chat-messages" aria-live="polite">
        <div class="message-log token-stream">{{ tokens }}</div>
        <div v-if="loading" class="badge" role="status">正在生成...</div>
        <form class="chat-input" @submit.prevent="submitQuery" aria-label="发送消息">
          <label class="sr-only" for="chat-message">问题</label>
          <textarea id="chat-message" v-model="messageInput" rows="3" placeholder="请输入筛选需求，例如：找在华东、有 AI 能力的中型制造企业"></textarea>
          <div class="filter-actions">
            <button class="button" type="submit" :disabled="loading">发送</button>
          </div>
        </form>
        <div v-if="companies.length" class="card-grid" aria-label="推荐企业列表">
          <article v-for="company in companies" :key="company.id" class="company-card" tabindex="0">
            <header>
              <h3>{{ company.name }}</h3>
              <p class="badge">得分 {{ company.score.toFixed(2) }}</p>
            </header>
            <p>{{ company.summary }}</p>
            <div class="company-tags" aria-label="行业">
              <span v-for="item in company.industry" :key="item" class="tag">{{ item }}</span>
            </div>
            <p v-if="company.size">规模：{{ company.size }}</p>
            <div class="company-tags" aria-label="地区">
              <span v-for="item in company.region" :key="item" class="tag">{{ item }}</span>
            </div>
            <div class="company-tags" aria-label="技术关键词">
              <span v-for="item in company.techKeywords" :key="item" class="tag">{{ item }}</span>
            </div>
            <footer>
              <small>命中依据：{{ company.reason }}</small>
            </footer>
          </article>
        </div>
      </section>
    </section>
  `
};
