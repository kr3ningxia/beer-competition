<template>
  <router-view />
</template>

<style>
:root {
  --app-viewport-width: 100vw;
  --app-viewport-height: 100dvh;
  --app-viewport-offset-left: 0px;
  --app-viewport-offset-top: 0px;
  color: #14213d;
  background: #f5f7fb;
  font-family: "Segoe UI", "PingFang SC", sans-serif;
}

* {
  box-sizing: border-box;
}

button,
input,
textarea,
select {
  font: inherit;
}

body {
  margin: 0;
  overflow-x: hidden;
  background: #f5f7fb;
}

input[type="number"] {
  appearance: textfield;
  -moz-appearance: textfield;
}

input[type="number"]::-webkit-outer-spin-button,
input[type="number"]::-webkit-inner-spin-button {
  margin: 0;
  appearance: none;
  -webkit-appearance: none;
}

.admin-shell input[type="date"],
.admin-shell input[type="datetime-local"] {
  color-scheme: dark;
}

.admin-shell input[type="date"]::-webkit-calendar-picker-indicator,
.admin-shell input[type="datetime-local"]::-webkit-calendar-picker-indicator {
  width: 16px;
  height: 16px;
  padding: 4px;
  margin-inline-start: 8px;
  cursor: pointer;
  border-radius: 6px;
  opacity: 1;
  background:
    rgba(216, 169, 53, 0.16)
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%23dfb94c' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M8 2v4M16 2v4M3 10h18M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z'/%3E%3C/svg%3E")
    center / 16px 16px
    no-repeat;
  transition: background-color 0.16s ease, box-shadow 0.16s ease;
}

.admin-shell input[type="date"]:hover::-webkit-calendar-picker-indicator,
.admin-shell input[type="date"]:focus::-webkit-calendar-picker-indicator,
.admin-shell input[type="datetime-local"]:hover::-webkit-calendar-picker-indicator,
.admin-shell input[type="datetime-local"]:focus::-webkit-calendar-picker-indicator {
  background-color: rgba(216, 169, 53, 0.28);
  box-shadow: 0 0 0 1px rgba(216, 169, 53, 0.22);
}

.admin-shell input[type="date"]:disabled::-webkit-calendar-picker-indicator,
.admin-shell input[type="datetime-local"]:disabled::-webkit-calendar-picker-indicator {
  cursor: not-allowed;
  opacity: 0.42;
}

a {
  color: inherit;
}

@media (max-width: 720px) {
  input:not([type]),
  input:is(
    [type="text"],
    [type="search"],
    [type="tel"],
    [type="email"],
    [type="url"],
    [type="password"],
    [type="number"],
    [type="date"],
    [type="time"],
    [type="datetime-local"]
  ),
  textarea,
  select,
  .el-input,
  .el-textarea,
  .el-select,
  .el-input__inner,
  .el-textarea__inner,
  .el-select__input,
  .el-select__selected-item,
  .el-select__placeholder {
    font-size: 16px !important;
  }

  .el-input__wrapper,
  .el-select__wrapper,
  .el-input-number {
    min-height: 44px;
  }

  .el-overlay-dialog {
    padding-top: max(12px, env(safe-area-inset-top));
    padding-right: 12px;
    padding-bottom: max(12px, env(safe-area-inset-bottom));
    padding-left: 12px;
  }

  .el-dialog:not(.is-fullscreen) {
    display: flex;
    flex-direction: column;
    width: 100% !important;
    max-width: 640px;
    max-height: calc(var(--app-viewport-height, 100dvh) - 24px);
    margin: auto;
  }

  .el-dialog:not(.is-fullscreen) .el-dialog__header,
  .el-dialog:not(.is-fullscreen) .el-dialog__footer {
    flex: 0 0 auto;
  }

  .el-dialog:not(.is-fullscreen) .el-dialog__body {
    min-height: 0;
    overflow-y: auto;
    overscroll-behavior: contain;
  }
}
</style>
