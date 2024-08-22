@import gg.jte.Content

@param Content content
@param String flashMessage = null

<html class="-no-dark-theme" lang="en">
<head>
  <title>Contacts app</title>
  <link rel="stylesheet" href="/static/missing.min.css">
  <link rel="stylesheet" href="/static/site.css">
  <script src="/static/js/htmx.min.js"></script>
  <script src="/static/js/_hyperscript.min.js"></script>
  <script src="/static/js/rsjs-menu.js"></script>
  <script defer src="/static/js/alpine.js"></script>
</head>
<body hx-boost="true">
<main>
  <header>
    <h1>
      <all-caps>contacts.app</all-caps>
      <sub-title>A demo contacts application</sub-title>
    </h1>
  </header>
  @if(flashMessage != null)
    <div class="flash">${flashMessage}</div>
  @endif
</main>
${content}
</body>
</html>
