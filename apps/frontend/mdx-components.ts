import { useMDXComponents as getDocsComponents } from "nextra-theme-docs";

type Components = ReturnType<typeof getDocsComponents>;

export function useMDXComponents(components?: Partial<Components>): Components {
  return { ...getDocsComponents(), ...components };
}
