import { DoAxiosWithErro } from "..";

export interface CurrentUserInfo {
  avatar: string;
  name: string;
  gender: number;
  age: number;
  region: string;
  address: string;
  phone: string;
  email: string;
  idCard: string;
  userId: string;
  doctorId?: string;
}

// 头像修改
export const updateUserAvatar = async (avatar: File) => {
  try {
    const formData = new FormData();
    formData.append("file", avatar, avatar.name); // 这里的 avatar.name 是文件的原始名称
    const res = await DoAxiosWithErro("/auth/updateAvatar", "post", formData, true, false);
    return res;
  } catch (err) {
    console.error(err);
  }
};

export const getUesrInfo = async (): Promise<CurrentUserInfo | undefined> => {
  try {
    const res = await DoAxiosWithErro<CurrentUserInfo>("/auth/currentUser", "get", {}, true, false);
    return res;
  } catch (err) {
    console.error(err);
  }
};

export const updatePassword = async (oldPassword: string, newPassword: string) => {
  try {
    const res = await DoAxiosWithErro(
      `/auth/updatePassword?oldPassword=${encodeURIComponent(oldPassword)}&newPassword=${encodeURIComponent(newPassword)}`,
      "post",
      {},
      true,
      false
    );
    return res;
  } catch (err) {
    console.error(err);
  }
};

export const updateUserInfo = async (
  name: string,
  gender: number,
  age: number,
  region: string,
  address: string,
  phone: string,
  idCard: string
) => {
  try {
    const res = await DoAxiosWithErro<CurrentUserInfo>(
      "/auth/updateInfo",
      "post",
      {
        name,
        gender,
        age,
        region,
        address,
        phone,
        idCard,
      },
      true,
      true
    );
    return res;
  } catch (err) {
    console.error(err);
  }
};
