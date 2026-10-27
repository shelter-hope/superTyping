import * as React from "react"
import { Slot } from "@radix-ui/react-slot"
import { cva, type VariantProps } from "class-variance-authority"

import { cn } from "@/utils"

/*
 * 按钮 = 排版元素，不是塑料药丸。
 * 默认实心墨黑（在 .dark 里自动反转成宣纸色），描边/幽灵用于次级动作。
 */
const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-[2px] border text-[13px] font-medium tracking-[0.02em] transition-all duration-150 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background disabled:pointer-events-none disabled:opacity-40 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0",
  {
    variants: {
      variant: {
        default:
          "border-transparent bg-foreground text-background hover:-translate-y-px hover:bg-foreground/90",
        accent:
          "border-transparent bg-accent text-accent-foreground hover:-translate-y-px hover:bg-accent/90",
        outline:
          "border-foreground/60 bg-transparent text-foreground hover:border-foreground hover:bg-foreground hover:text-background",
        secondary:
          "border-border bg-transparent text-foreground hover:border-foreground/50 hover:bg-foreground/[0.04]",
        destructive:
          "border-transparent bg-destructive text-destructive-foreground hover:bg-destructive/90",
        ghost:
          "border-transparent bg-transparent text-muted-foreground hover:bg-foreground/[0.05] hover:text-foreground",
        link: "border-transparent bg-transparent p-0 text-foreground underline decoration-accent decoration-2 underline-offset-4 hover:text-accent",
      },
      size: {
        default: "h-10 px-5 py-2",
        sm: "h-8 px-3 text-xs",
        lg: "h-12 px-7 text-[15px]",
        icon: "h-10 w-10",
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
