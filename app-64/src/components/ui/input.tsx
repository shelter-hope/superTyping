import * as React from "react";

import { cn } from "@/utils";

function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  return (
    <input
      type={type}
      data-slot="input"
      className={cn(
        "file:text-foreground placeholder:text-muted-foreground selection:bg-primary selection:text-primary-foreground flex h-9 w-full min-w-0 border border-input bg-[linear-gradient(180deg,hsl(216_17%_9%/0.92),hsl(216_17%_6%/0.96))] px-3 py-1 text-base text-foreground shadow-[inset_0_1px_8px_hsl(0_0%_0%/0.38)] transition-[border-color,box-shadow,background] outline-none file:inline-flex file:h-7 file:border-0 file:bg-transparent file:text-sm file:font-medium disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50 md:text-sm",
        "focus-visible:border-ring focus-visible:ring-ring/30 focus-visible:ring-[2px] focus-visible:shadow-[inset_0_1px_8px_hsl(0_0%_0%/0.38),0_0_18px_hsl(var(--primary)/0.14)]",
        "aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive",
        className
      )}
      {...props}
    />
  );
}

export { Input };
