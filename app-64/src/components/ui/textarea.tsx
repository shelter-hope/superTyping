import * as React from "react";

import { cn } from "@/utils";

const Textarea = React.forwardRef<
  HTMLTextAreaElement,
  React.TextareaHTMLAttributes<HTMLTextAreaElement>
>(({ className, ...props }, ref) => (
  <textarea
    ref={ref}
    data-slot="textarea"
    className={cn(
      "border-input placeholder:text-muted-foreground focus-visible:border-foreground aria-invalid:border-destructive flex min-h-16 w-full rounded-sm border bg-background px-3 py-2 text-sm transition-[border-color] outline-none disabled:cursor-not-allowed disabled:opacity-50",
      className
    )}
    {...props}
  />
));

Textarea.displayName = "Textarea";

export { Textarea };
