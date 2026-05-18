import { DoAxiosWithErro } from "..";
import myApi from "..";

// RAG文档输入结构
export interface RagDocument {
  text: string;
  metadata?: Record<string, unknown>;
}

// RAG检索结果
export interface RagSearchResult {
  id: string;
  text: string;
  metadata?: Record<string, unknown>;
}

/**
 * 添加RAG文档
 * @param documents 待入库的文档列表
 */
export const addRagDocuments = async (documents: RagDocument[]): Promise<number | undefined> => {
  try {
    const res = await DoAxiosWithErro<number>(
      "/ai/rag/documents",
      "post",
      { documents },
      true,
      true
    );
    return res;
  } catch (err) {
    console.error("添加RAG文档失败:", err);
    throw err;
  }
};

/**
 * 上传PDF文档并入库
 * @param file PDF文件
 */
export const uploadPdf = async (file: File): Promise<number> => {
  const formData = new FormData();
  formData.append("file", file);

  try {
    const token = localStorage.getItem("userToken") || "";
    const response = await myApi.post("/ai/rag/upload-pdf", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
        satoken: token,
        Authorization: `Bearer ${token}`,
      },
      timeout: 60000, // PDF处理可能较慢，设置60秒超时
    });

    if (response.data.code === 200) {
      return response.data.data as number;
    }
    throw new Error(response.data.message || "PDF上传失败");
  } catch (err) {
    console.error("PDF上传失败:", err);
    throw err;
  }
};

/**
 * RAG相似度检索
 * @param query 检索问题
 * @param topK 返回条数
 * @param similarityThreshold 相似度阈值
 */
export const searchRagDocuments = async (
  query: string,
  topK?: number,
  similarityThreshold?: number
): Promise<RagSearchResult[]> => {
  try {
    const res = await DoAxiosWithErro<RagSearchResult[]>(
      "/ai/rag/search",
      "post",
      {
        query,
        topK: topK ?? 5,
        similarityThreshold: similarityThreshold ?? 0.5,
      },
      true,
      true
    );
    return res ?? [];
  } catch (err) {
    console.error("RAG检索失败:", err);
    throw err;
  }
};
