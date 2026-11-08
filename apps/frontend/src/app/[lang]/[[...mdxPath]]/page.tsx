import { generateStaticParamsFor, importPage } from "nextra/pages";

import { useMDXComponents as getMDXComponents } from "../../../../mdx-components";

export const generateStaticParams = generateStaticParamsFor("mdxPath", "lang");

type Props = { params: Promise<{ mdxPath?: string[]; lang: string }> };

export async function generateMetadata(props: Props) {
  const { mdxPath, lang } = await props.params;
  const { metadata } = await importPage(mdxPath, lang);
  return metadata;
}

const Wrapper = getMDXComponents().wrapper!;

export default async function Page(props: Props) {
  const { mdxPath, lang } = await props.params;
  const { default: MDXContent, toc, metadata, sourceCode } = await importPage(mdxPath, lang);
  return (
    <Wrapper toc={toc} metadata={metadata} sourceCode={sourceCode}>
      <MDXContent {...props} params={await props.params} />
    </Wrapper>
  );
}
