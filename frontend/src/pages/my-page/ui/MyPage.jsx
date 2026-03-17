import "./MyPage.css";
import Header from "@/widgets/header/ui/Header";
import MyInfo from "@/features/user-profile/ui/MyInfo";
import MyPost from "@/widgets/my-post/ui/MyPost";
import { useAuthStore } from "@/features/auth/model/authStore";
import { useGetMe, useUpdateMe, useDeleteMe } from "@/features/user-profile/api/userApi";

const MyPage = () => {
  const logout = useAuthStore((s) => s.logout);

  // React Query로 내 정보 조회
  const { data: userData } = useGetMe();
  const userInfo = userData ? { id: userData.loginId, hp: userData.hp } : {};

  // 정보 수정 mutation
  const updateMe = useUpdateMe();
  // 회원 탈퇴 mutation
  const deleteMe = useDeleteMe();

  const onUpdate = async (pw, hp) => {
    await updateMe.mutateAsync({ pw, hp });
  };

  const onDelete = async () => {
    if (confirm("탈퇴 시 모든 정보가 삭제됩니다. 정말 탈퇴하시겠습니까?")) {
      try {
        await deleteMe.mutateAsync();
        onLogOut();
      } catch (error) {
        console.error("Failed to delete user:", error);
        alert("삭제에 실패했습니다.");
      }
    }
  };

  // logout()이 /login으로 이동까지 처리하지 않으므로 navigate 추가 불필요
  // ProtectedRoute가 isAuthenticated 반응으로 처리
  const onLogOut = () => {
    logout();
  };

  return (
    <div className="MyPage">
      <Header rightChild={"child"} />
      <div className="MyPage-container inner">
        <div className="MyInfo-container">
          <MyInfo
            userInfo={userInfo}
            onUpdate={onUpdate}
            onDelete={onDelete}
            onLogOut={onLogOut}
          />
        </div>
        <MyPost/>
      </div>
    </div>
  );
};
export default MyPage;
