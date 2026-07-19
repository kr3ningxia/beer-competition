<template>
  <div class="judge-app">
    <router-view />
    <SiteFilingFooter :with-bottom-nav="hasBottomNav" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SiteFilingFooter from '@/components/SiteFilingFooter.vue'

const route = useRoute()
const bottomNavExactPaths = ['/competitions', '/judged', '/profile']
const bottomNavPathPrefixes = [
  '/captain',
  '/q/',
  '/scan-result/',
  '/score-confirmation/',
  '/ranking-confirmation/',
]

const hasBottomNav = computed(() => (
  bottomNavExactPaths.includes(route.path)
  || bottomNavPathPrefixes.some((path) => route.path.startsWith(path))
))
</script>

<style>
:root {
  font-family: "Aptos", "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  color: #18222f;
  background: #eef1f0;
  font-synthesis: none;
  text-rendering: optimizeLegibility;
}

* {
  box-sizing: border-box;
}

.judge-app {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #eef1f0;
}

.judge-app > main {
  flex: 1;
  min-height: 0 !important;
}

html {
  background: #eef1f0;
}

body {
  min-width: 320px;
  margin: 0;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.86), rgba(238, 241, 240, 0.94)),
    repeating-linear-gradient(90deg, rgba(24, 34, 47, 0.035) 0 1px, transparent 1px 18px);
}

button,
input,
textarea {
  font: inherit;
}

button {
  cursor: pointer;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

a {
  color: inherit;
}

button:focus-visible,
a:focus-visible,
input:focus-visible,
textarea:focus-visible,
select:focus-visible {
  outline: 3px solid rgba(167, 85, 23, 0.28);
  outline-offset: 2px;
}

.app-shell {
  width: min(100%, 520px);
  min-height: 100vh;
  margin: 0 auto;
  padding: 14px 14px 88px;
}

.app-shell > * + * {
  margin-top: 12px;
}

.top-panel,
.card,
.sheet {
  border: 1px solid rgba(24, 34, 47, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 12px 30px rgba(24, 34, 47, 0.08);
}

.top-panel {
  padding: 18px;
  color: #f8fafc;
  background:
    linear-gradient(135deg, rgba(20, 31, 43, 0.98), rgba(50, 56, 45, 0.96)),
    linear-gradient(90deg, #141f2b, #32382d);
}

.eyebrow {
  margin: 0 0 6px;
  color: rgba(248, 250, 252, 0.74);
  font-size: 13px;
}

.page-title {
  margin: 0;
  font-size: 25px;
  font-weight: 750;
  letter-spacing: 0;
}

.muted {
  color: #667085;
}

.caption {
  margin: 6px 0 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.45;
}

.card {
  padding: 16px;
}

.section-title {
  margin: 0 0 12px;
  color: #26313d;
  font-size: 17px;
  font-weight: 750;
}

.button {
  min-height: 46px;
  border: 0;
  border-radius: 8px;
  padding: 12px 15px;
  font-weight: 750;
}

.button.primary {
  color: #fff;
  background: #a75517;
}

.button.secondary {
  color: #18222f;
  background: #e6ece8;
}

.button.dark {
  color: #fff;
  background: #1f2a37;
}

.button.danger {
  color: #fff;
  background: #b42318;
}

.button.full {
  width: 100%;
}

.field {
  display: block;
  margin-top: 14px;
  color: #344054;
  font-size: 14px;
  font-weight: 700;
}

.input,
.textarea,
.select {
  width: 100%;
  min-height: 46px;
  margin-top: 8px;
  border: 1px solid #cbd5d1;
  border-radius: 8px;
  padding: 11px 12px;
  color: #18222f;
  background: #fff;
  font-size: 16px;
  outline: none;
}

.textarea {
  min-height: 128px;
  resize: vertical;
  line-height: 1.55;
}

.input:focus,
.textarea:focus,
.select:focus {
  border-color: #a75517;
  box-shadow: 0 0 0 3px rgba(167, 85, 23, 0.14);
}

.pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  border: 1px solid rgba(24, 34, 47, 0.12);
  border-radius: 999px;
  padding: 5px 10px;
  color: #344054;
  background: #f7f8f6;
  font-size: 13px;
  font-weight: 750;
}

.status-ok {
  color: #067647;
  background: #ecfdf3;
  border-color: #abefc6;
}

.status-warn {
  color: #92400e;
  background: #fffaeb;
  border-color: #fedf89;
}

.status-lock {
  color: #475467;
  background: #f2f4f7;
  border-color: #d0d5dd;
}

.bottom-nav {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 20;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  width: min(100%, 568px);
  min-height: 60px;
  margin: 0 auto;
  border-top: 1px solid rgba(24, 34, 47, 0.12);
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(12px);
}

.nav-item {
  display: grid;
  grid-template-rows: 22px 17px;
  gap: 2px;
  place-items: center;
  align-content: center;
  min-height: 60px;
  color: #6f6b66;
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.15;
}

.nav-item svg {
  width: 20px;
  height: 20px;
  stroke: currentColor;
  stroke-width: 2.1;
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.nav-item span {
  display: block;
}

.nav-item.router-link-active,
.nav-item.is-active {
  color: #d17932;
  font-weight: 850;
}

.stack {
  display: grid;
  gap: 12px;
}

.inline {
  display: flex;
  gap: 10px;
  align-items: center;
}

.split {
  display: flex;
  gap: 10px;
  justify-content: space-between;
  align-items: center;
}

@media (min-width: 700px) {
  .app-shell {
    padding-top: 22px;
  }
}
</style>
