<template>
  <div class="page-card">
    <h2>客户开户</h2>
    <div class="split-grid">
      <section class="section-card">
        <h3>客户信息录入</h3>
        <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
        <p v-if="hintMsg" class="hint-msg">{{ hintMsg }}</p>
        <form @submit.prevent="onSubmit">
          <div>
            <label>姓名：</label>
            <input v-model="form.userName" maxlength="10" autocomplete="name" placeholder="2～10 个字符" />
          </div>
          <div>
            <label>证件类型：</label>
            <select v-model="form.idType" :disabled="dictLoading">
              <option value="">请选择</option>
              <option v-for="o in idTypes" :key="o.itemCode" :value="o.itemCode">
                {{ o.itemName }}（{{ o.itemCode }}）
              </option>
            </select>
          </div>
          <div>
            <label>证件号：</label>
            <input v-model="form.idNo" maxlength="30" autocapitalize="off" placeholder="依证件类型格式填写" />
          </div>
          <div>
            <label>账户类别：</label>
            <select v-model="form.cuacctCls" :disabled="dictLoading">
              <option value="">请选择</option>
              <option v-for="o in cuacctClasses" :key="o.itemCode" :value="o.itemCode">
                {{ o.itemName }}（{{ o.itemCode }}）
              </option>
            </select>
          </div>
          <div>
            <label>初始密码：</label>
            <input
              v-model="form.password"
              type="password"
              maxlength="32"
              autocomplete="new-password"
              placeholder="留空则默认 123456"
            />
          </div>
          <button type="submit" :disabled="loading">{{ loading ? "提交中…" : "提交开户" }}</button>
        </form>
      </section>

      <section class="section-card">
        <div class="section-head">
          <h3>客户列表</h3>
          <button type="button" class="secondary" @click="loadList">刷新</button>
        </div>
        <table>
          <thead>
            <tr>
              <th>客户名称</th>
              <th>客户代码</th>
              <th>证件号</th>
              <th>账户状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!rows.length && !listLoading">
              <td colspan="4">暂无数据，请先开户或点击刷新</td>
            </tr>
            <tr v-for="item in rows" :key="item.customerCode">
              <td>{{ item.userName }}</td>
              <td>{{ item.customerCode }}</td>
              <td>{{ item.idNo }}</td>
              <td>{{ statusLabel(item.cuacctStatus) }}</td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useAppStore } from "../../store";
import { listCustomersApi, registerCustomerApi, registerDictsApi } from "../../api/customer";
import { validateRegisterForm } from "../../utils/registerValidation";

const appStore = useAppStore();
const form = ref({
  userName: "",
  idType: "",
  idNo: "",
  cuacctCls: "",
  password: ""
});
const idTypes = ref([]);
const cuacctClasses = ref([]);
const dictLoading = ref(false);
const rows = ref([]);
const errorMsg = ref("");
const hintMsg = ref("");
const loading = ref(false);
const listLoading = ref(false);

function statusLabel(code) {
  const map = { 0: "正常", 1: "冻结", 8: "异常", 9: "注销" };
  return map[code] != null ? map[code] : code ?? "—";
}

async function loadDicts() {
  dictLoading.value = true;
  try {
    const res = await registerDictsApi();
    const body = res.data;
    if (!body.success || !body.data) {
      errorMsg.value = body.message || "加载字典失败";
      return;
    }
    idTypes.value = body.data.idTypes || [];
    cuacctClasses.value = body.data.cuacctClasses || [];
  } catch (e) {
    errorMsg.value = e.message || "加载字典失败";
  } finally {
    dictLoading.value = false;
  }
}

async function loadList() {
  errorMsg.value = "";
  listLoading.value = true;
  try {
    const res = await listCustomersApi();
    const body = res.data;
    if (!body.success) {
      errorMsg.value = body.message || "加载列表失败";
      return;
    }
    rows.value = body.data || [];
    appStore.syncCustomersFromApi(rows.value);
  } catch (e) {
    if (e.status === 401 || e.status === 403) {
      errorMsg.value = e.message || "无权限或未登录，请先使用操作员账号登录";
    } else {
      errorMsg.value = e.message || "加载列表失败";
    }
  } finally {
    listLoading.value = false;
  }
}

const onSubmit = async () => {
  errorMsg.value = "";
  hintMsg.value = "";
  const localErr = validateRegisterForm(form.value);
  if (localErr) {
    errorMsg.value = localErr;
    return;
  }
  loading.value = true;
  try {
    const payload = {
      userName: form.value.userName.trim(),
      idType: form.value.idType,
      idNo: form.value.idNo.trim(),
      cuacctCls: form.value.cuacctCls
    };
    if (form.value.password?.trim()) {
      payload.password = form.value.password.trim();
    }
    const res = await registerCustomerApi(payload);
    const body = res.data;
    if (!body.success) {
      errorMsg.value = body.message || "开户失败";
      return;
    }
    hintMsg.value = `开户成功，客户代码：${body.data.customerCode}`;
    appStore.addCustomer({
      customerCode: body.data.customerCode,
      userName: body.data.userName || payload.userName
    });
    await loadList();
  } catch (e) {
    if (e.status === 403) {
      errorMsg.value = "需要操作员权限：请使用 operator 登录后再开户";
    } else if (e.status === 401) {
      errorMsg.value = "登录已失效，请重新登录";
    } else {
      errorMsg.value = e.message || "开户失败";
    }
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  await loadDicts();
  await loadList();
});
</script>

<style scoped>
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.section-head h3 {
  margin: 0;
}
.error-msg {
  color: #a63d3d;
  font-size: 14px;
  margin: 0 0 10px;
}
.hint-msg {
  color: #3d6a35;
  font-size: 14px;
  margin: 0 0 10px;
}
button.secondary {
  padding: 6px 14px;
  font-size: 13px;
}
select {
  width: 280px;
  max-width: 100%;
}
</style>
