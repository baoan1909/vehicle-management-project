import type { HTMLAttributes, PropsWithChildren } from "react";

type UserGuideAnchorProps = PropsWithChildren<HTMLAttributes<HTMLDivElement> & { anchorKey: string }>;

export function UserGuideAnchor({ anchorKey, children, ...props }: UserGuideAnchorProps) {
  return (
    <div data-user-guide-anchor={anchorKey} {...props}>
      {children}
    </div>
  );
}

export function userGuideAnchorProps(anchorKey: string) {
  return { "data-user-guide-anchor": anchorKey } as const;
}
