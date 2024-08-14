@import gg.jte.Content

@param Content content
@param String flashMessage = null

<head>
  <title>Contacts app</title>
  <link rel="stylesheet" href="/static/missing.min.css">
  <link rel="stylesheet" href="/static/site.css">
</head>
<body>
<main>
  <header>
    <h1>CONTACTS.APP - A demo contacts application</h1>
  </header>
  @if(flashMessage != null)
    <div class="flash">${flashMessage}</div>
  @endif
</main>
${content}
</body>
