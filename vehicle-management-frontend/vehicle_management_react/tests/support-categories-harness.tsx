import React, { useState } from "react";
import { createRoot } from "react-dom/client";
import { CreateSupportTicketDialog } from "@/features/support/components/CreateSupportTicketDialog";
import { createSupportTicket } from "@/features/support/api/supportApi";
import "@/app/styles/tailwind.css";
import "@/app/styles/globals.css";

function Harness() {
  const [open, setOpen] = useState(true);
  const [created, setCreated] = useState(false);
  return <>
    <button onClick={() => setOpen(true)}>Mở form hỗ trợ</button>
    {created ? <p role="status">Tạo phiếu thành công</p> : null}
    <CreateSupportTicketDialog open={open} onClose={() => setOpen(false)} onCreated={() => setCreated(true)}
      createTicket={async (body, key) => (await createSupportTicket(body, key)).data} />
  </>;
}
createRoot(document.getElementById("root")!).render(<Harness />);
