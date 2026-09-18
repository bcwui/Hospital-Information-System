import { DoAxiosWithErro } from "..";
import type { BillVO } from "../patient/billing";

export const getAllBills = async (status?: number): Promise<BillVO[]> => {
  try {
    return await DoAxiosWithErro<BillVO[]>("/admin/bills", "get", { status } as Record<string, unknown>, true, false) ?? [];
  } catch (err) {
    console.error("获取全部账单失败:", err);
    throw err;
  }
};

export const getBillDetail = async (billId: number): Promise<BillVO> => {
  try {
    return await DoAxiosWithErro<BillVO>(`/admin/bills/${billId}`, "get", {}, true, false);
  } catch (err) {
    console.error("获取账单详情失败:", err);
    throw err;
  }
};

export const markAsPaid = async (billId: number, paymentMethod: string): Promise<boolean> => {
  try {
    return await DoAxiosWithErro<boolean>(`/admin/bills/${billId}/mark-paid`, "put", { paymentMethod } as Record<string, unknown>, true, true);
  } catch (err) {
    console.error("标记已付失败:", err);
    throw err;
  }
};
