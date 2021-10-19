import React from 'react';
import Head from '@docusaurus/Head';


// Default implementation, that you can customize
function Root({children}) {
  return <>
    <Head>
      <script>
        {`(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':
  new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],
  j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src=
  'https://www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);
  })(window,document,'script','dataLayer','GTM-5TZTCGF');`}
    </script>
    </Head>
    {children}
    <noscript>
      
      <iframe src="https://www.googletagmanager.com/ns.html?id=GTM-5TZTCGF"
height="0" width="0" style="display:none;visibility:hidden"></iframe>
</noscript>
  </>;
}

export default Root;