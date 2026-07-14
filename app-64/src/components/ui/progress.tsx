import * as React from "react";
import * as ProgressPrimitive from "@radix-ui/react-progress";

import { cn } from "@/utils";

function Progress({
  className,
  value,
  ...props
}: React.ComponentProps<typeof ProgressPrimitive.Root>) {
  return (
    <ProgressPrimitive.Root
      data-slot="progress"
      className={cn(
        "relative h-2 w-full overflow-hidden border border-border bg-[linear-gradient(90deg,hsl(216_18%_7%),hsl(216_14%_14%))] shadow-[inset_0_0_8px_hsl(0_0%_0%/0.55)]",
        className
      )}
      {...props}
    >
      <ProgressPrimitive.Indicator
        data-slot="progress-indicator"
        className="h-full w-full flex-1 bg-[linear-gradient(90deg,hsl(var(--primary)/0.38),hsl(var(--primary)/0.92),hsl(var(--primary)/0.52))] shadow-[0_0_12px_hsl(var(--primary)/0.35)] transition-all duration-200"
        style={{ transform: `translateX(-${100 - (value || 0)}%)` }}
      />
    </ProgressPrimitive.Root>
  );
}

export { Progress };
