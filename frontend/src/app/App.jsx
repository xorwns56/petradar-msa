import "@/app/App.css";
import AppProvider from "@/app/providers";
import AppRouter from "@/app/router";
import SideBar from "@/widgets/sidebar/ui/SideBar";
import { SocketManager } from "@/features/notification/model/SocketManager";

function App() {
  return (
    <>
      <AppProvider>
        <SocketManager />
        <SideBar />
        <AppRouter />
      </AppProvider>
    </>
  );
}

export default App;
