import { Routes, Route } from "react-router-dom";
import ProtectedRoute from "@/shared/ui/protected-route/ProtectedRoute";

// 페이지 컴포넌트 임포트
import HomePage from "@/pages/home/ui/HomePage";
import LoginPage from "@/pages/login/ui/LoginPage";
import RegisterPage from "@/pages/register/ui/RegisterPage";
import MyPage from "@/pages/my-page/ui/MyPage";
import MissingDeclarationPage from "@/pages/missing-declaration/ui/MissingDeclarationPage";
import MissingReportPage from "@/pages/missing-report/ui/MissingReportPage";
import MissingRevisePage from "@/pages/missing-revise/ui/MissingRevisePage";
import MissingListPage from "@/pages/missing-list/ui/MissingListPage";
import ShelterListPage from "@/pages/shelter-list/ui/ShelterListPage";
import ShelterAnimalListPage from "@/pages/shelter-animal-list/ui/ShelterAnimalListPage";
import NotFoundPage from "@/pages/not-found/ui/NotFoundPage";

// 라우트 정의
const AppRouter = () => {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />

      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      {/* 인증 필요 라우트 */}
      <Route path="/myPage" element={<ProtectedRoute><MyPage /></ProtectedRoute>} />
      <Route
        path="/missingDeclaration"
        element={<ProtectedRoute><MissingDeclarationPage /></ProtectedRoute>}
      />
      <Route
        path="/missingReport/:petMissingId"
        element={<ProtectedRoute><MissingReportPage /></ProtectedRoute>}
      />
      <Route
        path="/missingRevise/:petMissingId"
        element={<ProtectedRoute><MissingRevisePage /></ProtectedRoute>}
      />

      {/* 비인증 접근 가능 라우트 */}
      <Route path="/missingList" element={<MissingListPage />} />
      <Route path="/shelterList" element={<ShelterListPage />} />
      <Route
        path="/shelter/:name/:addr"
        element={<ShelterAnimalListPage />}
      />

      <Route path="/*" element={<NotFoundPage />} />
    </Routes>
  );
};

export default AppRouter;
