import { DoAxiosWithErro } from "..";

export interface DrugVO {
  drugId: number;
  drugName: string;
  specification: string;
  manufacturer: string;
  unit: string;
  unitPrice: number;
  stockQuantity: number;
  description: string;
  isActive: number;
  stockStatus: string;
  createTime: string;
}

export interface DrugDTO {
  drugId?: number;
  drugName: string;
  specification?: string;
  manufacturer?: string;
  unit?: string;
  unitPrice?: number;
  stockQuantity?: number;
  description?: string;
  isActive?: number;
}

export interface DrugStockDTO {
  quantity: number;
  remark?: string;
}

export const listDrugs = async (params?: { name?: string; isActive?: number }): Promise<DrugVO[]> => {
  try {
    return await DoAxiosWithErro<DrugVO[]>("/admin/drugs", "get", params as Record<string, unknown>, true, false) ?? [];
  } catch (err) {
    console.error("获取药品列表失败:", err);
    throw err;
  }
};

export const getDrug = async (drugId: number): Promise<DrugVO> => {
  try {
    return await DoAxiosWithErro<DrugVO>(`/admin/drugs/${drugId}`, "get", {}, true, false);
  } catch (err) {
    console.error("获取药品详情失败:", err);
    throw err;
  }
};

export const createDrug = async (dto: DrugDTO): Promise<DrugVO> => {
  try {
    return await DoAxiosWithErro<DrugVO>("/admin/drugs", "post", dto as unknown as Record<string, unknown>, true, true);
  } catch (err) {
    console.error("创建药品失败:", err);
    throw err;
  }
};

export const updateDrug = async (dto: DrugDTO): Promise<DrugVO> => {
  try {
    return await DoAxiosWithErro<DrugVO>("/admin/drugs", "put", dto as unknown as Record<string, unknown>, true, true);
  } catch (err) {
    console.error("更新药品失败:", err);
    throw err;
  }
};

export const deleteDrug = async (drugId: number): Promise<boolean> => {
  try {
    return await DoAxiosWithErro<boolean>(`/admin/drugs/${drugId}`, "delete", {}, true, false);
  } catch (err) {
    console.error("删除药品失败:", err);
    throw err;
  }
};

export const stockIn = async (drugId: number, dto: DrugStockDTO): Promise<DrugVO> => {
  try {
    return await DoAxiosWithErro<DrugVO>(`/admin/drugs/${drugId}/stock-in`, "post", dto as unknown as Record<string, unknown>, true, true);
  } catch (err) {
    console.error("药品入库失败:", err);
    throw err;
  }
};

export const stockOut = async (drugId: number, dto: DrugStockDTO): Promise<DrugVO> => {
  try {
    return await DoAxiosWithErro<DrugVO>(`/admin/drugs/${drugId}/stock-out`, "post", dto as unknown as Record<string, unknown>, true, true);
  } catch (err) {
    console.error("药品出库失败:", err);
    throw err;
  }
};
