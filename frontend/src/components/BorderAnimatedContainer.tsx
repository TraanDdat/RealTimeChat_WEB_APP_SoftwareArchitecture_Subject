import type { ReactNode } from "react";

interface BorderAnimatedContainerProps {
  children: ReactNode;
}

function BorderAnimatedContainer({ children }: BorderAnimatedContainerProps) {
  return (
    <div className="w-full h-full [background:linear-gradient(45deg,#ffffff,#f5f9ff_50%,#ffffff)_padding-box,conic-gradient(from_var(--border-angle),#dce8f5_80%,#0068ff_86%,#7db4ff_90%,#0068ff_94%,#dce8f5)_border-box] rounded-2xl border border-transparent animate-border flex overflow-hidden">
      {children}
    </div>
  );
}
export default BorderAnimatedContainer;