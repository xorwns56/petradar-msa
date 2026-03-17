import { QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import queryClient from "@/shared/config/queryClient";

// QueryClientProvider + BrowserRouter 합성 프로바이더
const AppProvider = ({ children }) => {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        {children}
      </BrowserRouter>
    </QueryClientProvider>
  );
};

export default AppProvider;
