<template>
  <router-view />
</template>

<style>
:root {
  color: #14213d;
  background: #f5f7fb;
  font-family: "Segoe UI", "PingFang SC", sans-serif;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
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
</style>
