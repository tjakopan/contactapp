@import hr.tjakopan.contactapp.ValidContact

@param ValidContact contact

@template.layout(content = @`
  <h1>${contact.firstName()} ${contact.lastName()}</h1>
  <div>
    <div>Phone: ${contact.phone()}</div>
    <div>Email: ${contact.email()}</div>
  </div>
  <p>
    <a href="/contacts/${contact.id()}/edit">Edit</a>
    <a href="/contacts">Back</a>
  </p>
`)
