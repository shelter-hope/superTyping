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
      "border-input placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-ring/30 aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive flex min-h-16 w-full rounded-[1.25rem] border border-primary/30 bg-card/80 px-3 py-2 text-base transition-[border-color,box-shadow] outline-none focus-visible:ring-[2px] disabled:cursor-not-allowed disabled:opacity-50 md:text-sm",
      className
    )}
    {...props}
  />
));

Textarea.displayName = "Textarea";

export { Textarea };
