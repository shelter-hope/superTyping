import * as React from "react"
import { Slot } from "@radix-ui/react-slot"
import { cva, type VariantProps } from "class-variance-authority"

import { cn } from "@/utils"

const buttonVariants = cva(
  "relative inline-flex items-center justify-center gap-2 overflow-hidden whitespace-nowrap border text-sm font-bold uppercase tracking-[0.08em] transition-all duration-150 [clip-path:polygon(0_0,calc(100%-8px)_0,100%_8px,100%_100%,8px_100%,0_calc(100%-8px))] focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-40 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0 before:absolute before:inset-y-0 before:left-0 before:w-1 before:bg-primary/70 before:shadow-[0_0_14px_hsl(var(--primary)/0.45)] after:absolute after:inset-y-0 after:-left-1/3 after:w-1/3 after:skew-x-[-18deg] after:bg-primary/10 after:transition-[left] after:duration-200 hover:after:left-full active:translate-y-px",
  {
    variants: {
      variant: {
        default:
          "border-primary/55 bg-[linear-gradient(180deg,hsl(207_20%_23%/0.95),hsl(216_20%_10%/0.98))] text-primary shadow-[inset_0_1px_0_hsl(0_0%_100%/0.08),0_10px_24px_hsl(216_35%_2%/0.45)] hover:border-primary hover:text-primary-foreground hover:shadow-hover",
        destructive:
          "border-destructive/60 bg-[linear-gradient(180deg,hsl(3_24%_25%/0.9),hsl(216_18%_9%/0.98))] text-destructive-foreground hover:border-destructive hover:shadow-[0_0_18px_hsl(var(--destructive)/0.22)]",
        outline:
          "border-border bg-[linear-gradient(180deg,hsl(216_14%_16%/0.85),hsl(216_18%_8%/0.95))] text-foreground hover:border-primary/70 hover:text-primary hover:shadow-hover",
        secondary:
          "border-border bg-secondary/70 text-secondary-foreground hover:border-primary/45 hover:bg-secondary",
        ghost: "border-transparent bg-transparent text-muted-foreground before:hidden hover:border-border hover:bg-muted/50 hover:text-foreground",
        link: "border-transparent bg-transparent text-primary before:hidden underline-offset-4 hover:text-foreground hover:underline",
      },
      size: {
        default: "h-9 px-4 py-2",
        sm: "h-8 px-3 text-xs",
        lg: "h-11 px-8",
        icon: "h-9 w-9",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : "button"
    return (
      <Comp
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        {...props}
      />
    )
  }
)
Button.displayName = "Button"

export { Button, buttonVariants }
