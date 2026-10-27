import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";

import { cn } from "@/utils";

const badgeVariants = cva(
  "inline-flex w-fit shrink-0 items-center justify-center gap-1 overflow-hidden border px-2 py-0.5 text-xs font-bold uppercase tracking-[0.08em] whitespace-nowrap transition-[color,box-shadow,border-color] focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[2px] aria-invalid:border-destructive aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 [&>svg]:pointer-events-none [&>svg]:size-3",
  {
    variants: {
      variant: {
        default:
          "border-foreground bg-foreground text-background [a&]:hover:bg-foreground/90",
        secondary:
          "border-border bg-muted text-foreground [a&]:hover:border-foreground/40",
        destructive:
          "border-destructive/50 bg-transparent text-destructive focus-visible:ring-destructive/20",
        outline:
          "border-border bg-transparent text-muted-foreground [a&]:hover:border-foreground/40 [a&]:hover:text-foreground",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
);

function Badge({
  className,
  variant,
  asChild = false,
  ...props
}: React.ComponentProps<"span"> &
  VariantProps<typeof badgeVariants> & { asChild?: boolean }) {
  const Comp = asChild ? Slot : "span";

  return (
    <Comp
      data-slot="badge"
      className={cn(badgeVariants({ variant }), className)}
      {...props}
    />
  );
}

export { Badge, badgeVariants };
