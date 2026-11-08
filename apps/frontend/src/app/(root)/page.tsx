export const metadata = { title: "Restless" };

const pick = `location.replace(/^ru\\b/i.test(navigator.language) ? "/ru/" : "/en/")`;

export default function Root() {
  return (
    <>
      <meta httpEquiv="refresh" content="0;url=/en/" />
      <script dangerouslySetInnerHTML={{ __html: pick }} />
      <noscript>
        <a href="/en/">English</a> · <a href="/ru/">Русский</a>
      </noscript>
    </>
  );
}
