import { defineStore } from "pinia";

export const useAppStore = defineStore("app", {
  state: () => ({
    token: "",
    operatorName: "",
    customers: [],
    selectedCustomerCode: ""
  }),
  actions: {
    login(token, operatorName) {
      this.token = token;
      this.operatorName = operatorName || "";
    },
    logout() {
      this.token = "";
      this.operatorName = "";
      this.customers = [];
      this.selectedCustomerCode = "";
      localStorage.removeItem("token");
    },
    addCustomer(customer) {
      const code = String(customer.customerCode);
      this.customers.unshift({ ...customer, customerCode: code });
    },
    setCustomers(list) {
      this.customers = (list || []).map((c) => ({
        customerCode: String(c.customerCode),
        userName: c.userName
      }));
    },
    setSelectedCustomerCode(code) {
      this.selectedCustomerCode = code != null ? String(code) : "";
    },
    syncCustomersFromApi(rows) {
      if (!rows?.length) {
        this.customers = [];
        return;
      }
      this.setCustomers(rows);
      const codes = rows.map((r) => String(r.customerCode));
      if (!this.selectedCustomerCode || !codes.includes(this.selectedCustomerCode)) {
        this.selectedCustomerCode = codes[0];
      }
    }
  }
});
