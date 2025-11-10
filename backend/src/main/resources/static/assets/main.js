import { createApp, h } from 'https://unpkg.com/vue@3/dist/vue.esm-browser.prod.js';
import { createRouter, createWebHashHistory } from 'https://unpkg.com/vue-router@4/dist/vue-router.esm-browser.prod.js';
import ChatModule from '../modules/chat/ChatModule.js';
import AboutModule from '../modules/about/AboutModule.js';

const routes = [
  { path: '/', redirect: '/chat' },
  { path: '/chat', component: ChatModule },
  { path: '/about', component: AboutModule }
];

const router = createRouter({
  history: createWebHashHistory(),
  routes
});

const AppShell = {
  setup() {
    return () => h('div', { class: 'app-shell' }, [
      h('header', { role: 'banner' }, [
        h('nav', { class: 'navbar', 'aria-label': 'Primary navigation' }, [
          h('div', { class: 'nav-brand' }, 'Dify Enterprise Filter'),
          h('ul', { class: 'nav-links' }, [
            h('li', {}, [h('a', { href: '#/chat', class: router.currentRoute.value.path === '/chat' ? 'router-link-active' : '' }, '对话筛选')]),
            h('li', {}, [h('a', { href: '#/about', class: router.currentRoute.value.path === '/about' ? 'router-link-active' : '' }, '占位模块')])
          ])
        ])
      ]),
      h('main', { role: 'main' }, [h('router-view')])
    ]);
  }
};

const app = createApp(AppShell);
app.use(router);
app.mount('#app');
