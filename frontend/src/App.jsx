import "./App.css";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import Home from "./pages/Home";

import Login from "./pages/Login";
import Register from "./pages/Register";
import MyPage from "./pages/MyPage";

import MissingReport from "./pages/MissingReport";
import MissingDeclaration from "./pages/MissingDeclaration";
import MissingRevise from "./pages/MissingRevise";
import MissingList from "./pages/MissingList";

import ShelterList from "./pages/ShelterList";
import ShelterAnimalList from "./pages/ShelterAnimalList";
import NotFound from "./pages/NotFound";
import SideBar from "./components/SideBar";

import { SidebarProvider } from "./hooks/SidebarContext";
import { ModalProvider } from "./hooks/ModalContext";
import { AuthProvider } from "./contexts/AuthContext"
import ProtectedRoute from "./shared/components/ProtectedRoute"

function App() {
  return (
    <>
            <BrowserRouter>
            <AuthProvider>
              <SidebarProvider>
                <SideBar />
                <ModalProvider>
                  <Routes>
                    <Route path="/" element={<Home />} />

                    <Route path="/login" element={<Login />} />
                    <Route path="/register" element={<Register />} />

                    {/* 인증 필요 라우트 */}
                    <Route path="/myPage" element={<ProtectedRoute><MyPage /></ProtectedRoute>} />
                    <Route
                      path="/missingDeclaration"
                      element={<ProtectedRoute><MissingDeclaration /></ProtectedRoute>}
                    />
                    <Route
                      path="/missingReport/:petMissingId"
                      element={<ProtectedRoute><MissingReport /></ProtectedRoute>}
                    />
                    <Route
                      path="/missingRevise/:petMissingId"
                      element={<ProtectedRoute><MissingRevise /></ProtectedRoute>}
                    />

                    {/* 비인증 접근 가능 라우트 */}
                    <Route path="/missingList" element={<MissingList />} />
                    <Route path="/shelterList" element={<ShelterList />} />
                    <Route
                      path="/shelter/:name/:addr"
                      element={<ShelterAnimalList />}
                    />

                    <Route path="/*" element={<NotFound />} />
                  </Routes>
                </ModalProvider>
              </SidebarProvider>
            </AuthProvider>
            </BrowserRouter>
    </>
  );
}

export default App;
