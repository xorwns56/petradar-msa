import "@/style/SideBar.css";
import Button from "@/shared/ui/button/Button";
import ReportAlertBox from "@/features/notification/ui/ReportAlertBox";
import MissingAlertBox from "@/features/notification/ui/MissingAlertBox";
import { useSidebarStore } from "@/widgets/sidebar/model/sidebarStore";
import { useNotificationStore } from "@/features/notification/model/notificationStore";
import { useNavigate } from "react-router-dom";
import { useDeleteNotification } from "@/features/notification/api/notificationApi";

const SideBar = () => {
  const nav = useNavigate();
  const { isActive, toggleSidebar } = useSidebarStore();
  const alerts = useNotificationStore((s) => s.alerts);
  const deleteNotification = useDeleteNotification();

  const onAlertClose = async (id) => {
    deleteNotification.mutate(id);
  };

  return (
    <div className={`SideBar ${isActive ? "active" : ""}`}>
      <div className="close-btn" onClick={toggleSidebar}>
        <Button text={"X"} type={"Circle"} />
      </div>
      {alerts.map((alert) =>
          alert.postType === "missing" ? (
            <MissingAlertBox
              key={`${alert.postType}_${alert.id}`}
              {...alert}
              onAlertClick={() => nav("/missingList")}
              onAlertClose={() => onAlertClose(alert.id)}
            />
          ) : (
            <ReportAlertBox
              key={`${alert.postType}_${alert.id}`}
              {...alert}
              currentUser={alert.receiverId}
              onAlertClick={() => nav("/myPage")}
              onAlertClose={() => onAlertClose(alert.id)}
            />
          )
        )}
    </div>
  );
};
export default SideBar;
