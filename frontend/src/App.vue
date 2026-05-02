<template>
  <router-view />
</template>

<style>
:root {
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  color: #1f1f1f;
  background: #f5f1e8;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  background: linear-gradient(180deg, #faf7ef, #f3eee3);
}

#app {
  min-height: 100vh;
}

.page-card {
  background: #fff;
  border: 1px solid #ede5d7;
  border-radius: 14px;
  padding: 18px;
  box-shadow: 0 8px 20px rgba(33, 28, 18, 0.08);
}

.split-grid {
  display: grid;
  grid-template-columns: minmax(320px, 430px) 1fr;
  gap: 14px;
  align-items: start;
}

.section-card {
  border: 1px solid #eee2cc;
  border-radius: 12px;
  background: #fffcf6;
  padding: 14px;
}

h2 {
  margin: 0 0 12px;
  color: #3a2f1d;
}

h3 {
  margin: 16px 0 10px;
  color: #5c4a2c;
}

form > div {
  margin-bottom: 10px;
}

label {
  display: inline-block;
  width: 96px;
  color: #63543a;
}

input,
select {
  width: 280px;
  max-width: 100%;
  padding: 8px 10px;
  border: 1px solid #d8ccb6;
  border-radius: 8px;
  background: #fffdfa;
}

button {
  border: 0;
  padding: 8px 14px;
  border-radius: 8px;
  cursor: pointer;
  color: #fff;
  background: linear-gradient(135deg, #c7a35a, #b88a3b);
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease,
    filter 0.22s ease,
    background 0.22s ease;
  box-shadow: 0 2px 8px rgba(120, 90, 40, 0.18);
}

button:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(140, 105, 45, 0.38);
  filter: brightness(1.06);
}

button:not(:disabled):active {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(120, 90, 40, 0.28);
  filter: brightness(0.98);
  transition-duration: 0.08s;
}

button:disabled {
  opacity: 0.62;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

button:focus-visible {
  outline: 2px solid #c9a24d;
  outline-offset: 2px;
}

button.secondary {
  background: linear-gradient(135deg, #ebdfc8, #dfc9a4);
  color: #4a3b22;
  box-shadow: 0 2px 6px rgba(90, 75, 45, 0.12);
}

button.secondary:not(:disabled):hover {
  background: linear-gradient(135deg, #f2e8d4, #e5d2ad);
  filter: brightness(1.04);
  box-shadow: 0 6px 14px rgba(100, 82, 45, 0.22);
}

pre {
  margin-top: 12px;
  padding: 10px;
  background: #29231a;
  color: #f6efe2;
  border-radius: 10px;
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  margin-top: 8px;
}

th,
td {
  border-bottom: 1px solid #eee6d8;
  padding: 10px 8px;
  text-align: left;
  font-size: 14px;
}

th {
  background: #f7f1e3;
  color: #5f4c2c;
}

tbody tr:nth-child(even) {
  background: #fcf8ef;
}

tbody tr:hover {
  background: #f5ebd9;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  color: #64553a;
  font-size: 13px;
}

.pagination .page-btn {
  min-width: 32px;
  padding: 4px 8px;
  border-radius: 6px;
  border: 1px solid #d8c7ab;
  background: #fff;
  color: #5b492f;
  box-shadow: 0 1px 4px rgba(80, 65, 35, 0.08);
}

.pagination .page-btn:not(:disabled):hover {
  transform: translateY(-1px);
  filter: none;
  border-color: #c4a86a;
  background: #fffef8;
  box-shadow: 0 4px 10px rgba(100, 82, 40, 0.15);
}

.pagination .page-btn:not(:disabled):active {
  transform: translateY(0);
  box-shadow: 0 1px 4px rgba(80, 65, 35, 0.1);
  transition-duration: 0.08s;
}

.pagination .page-btn.active {
  border-color: #bc9550;
  background: #ead8b7;
  box-shadow: 0 2px 8px rgba(160, 125, 55, 0.2);
}

.pagination .page-btn.active:hover {
  filter: brightness(1.03);
}

.tag-up {
  color: #d03a2f;
}

.tag-down {
  color: #1a8f47;
}

@media (max-width: 1024px) {
  .split-grid {
    grid-template-columns: 1fr;
  }
}
</style>
