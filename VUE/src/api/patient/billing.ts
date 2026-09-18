import { DoAxiosWithErro } from "..";

export interface BillItemVO {
  itemId: number;
  itemType: string;
  itemTypeDesc: string;
  itemName: string;
  unitPrice: number;
  quantity: number;
  amount: number;
  relatedId: number;
}

export interface BillVO {
  billId: number;
  appointmentId: number;
  patientId: number;
  patientName: string;
  totalAmount: number;
  paidAmount: number;
  unpaidAmount: number;
  status: number;
  statusDesc: string;
  paymentMethod: string;
  createTime: string;
  payTime: string;
  items: BillItemVO[];
}

export interface InvoiceVO {
  billId: number;
  patientName: string;
  totalAmount: number;
  paymentMethod: string;
  statusDesc: string;
  payTime: string;
  createTime: string;
  items: BillItemVO[];
}

export const getMyBills = async (): Promise<BillVO[]> => {
  try {
    return await DoAxiosWithErro<BillVO[]>("/patient/bills", "get", {}, true, false) ?? [];
  } catch (err) {
    console.error("获取账单失败:", err);
    throw err;
  }
};

export const getBillDetail = async (billId: number): Promise<BillVO> => {
  try {
    return await DoAxiosWithErro<BillVO>(`/patient/bills/${billId}`, "get", {}, true, false);
  } catch (err) {
    console.error("获取账单详情失败:", err);
    throw err;
  }
};

export const payBill = async (billId: number, paymentMethod: string): Promise<boolean> => {
  try {
    return await DoAxiosWithErro<boolean>(`/patient/bills/${billId}/pay`, "post", { paymentMethod } as Record<string, unknown>, true, true);
  } catch (err) {
    console.error("支付失败:", err);
    throw err;
  }
};

export const getInvoice = async (billId: number): Promise<InvoiceVO> => {
  try {
    return await DoAxiosWithErro<InvoiceVO>(`/patient/bills/${billId}/invoice`, "get", {}, true, false);
  } catch (err) {
    console.error("获取发票失败:", err);
    throw err;
  }
};
