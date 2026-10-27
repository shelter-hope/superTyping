import * as React from "react";

import { cn } from "@/utils";

function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  return (
    <input
      type={type}
      data-slot="input"
      className={cn(
        "flex h-10 w-full min-w-0 rounded-[2px] border border-border bg-transparent px-3 py-2 text-[14px] text-foreground outline-none transition-[border-color,background]",
        "placeholder:text-muted-foreground/70 placeholder:tracking-[0.02em]",
        "selection:bg-accent selection:text-accent-foreground",
        "file:mr-2 file:inline-flex file:h-7 file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-foreground",
        "hover:border-foreground/35",
        "focus-visible:border-foreground focus-visible:bg-foreground/[0.02]",
        "disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50",
        "aria-invalid:border-destructive",
        className
      )}
      {...props}
    />
  );
}

export { Input };
