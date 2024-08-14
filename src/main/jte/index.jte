@import java.util.List
@import hr.tjakopan.contactapp.ValidContact

@param String q = null
@param List<ValidContact> contacts
@param String flashMessage = null

@template.layout(flashMessage = flashMessage, content = @`
  <form action="/contacts" method="get" class="tool-bar">
    <label for="search">Search term</label>
    <input id="search" type="search" name="q" value="${q}">
    <input type="submit" value="Search">
  </form>

  <table>
    <thead>
    <tr>
      <th>First</th>
      <th>Last</th>
      <th>Phone</th>
      <th>Email</th>
      <th></th>
    </tr>
    </thead>
    <tbody>
    @for(final ValidContact contact : contacts)
      <tr>
        <td>${contact.firstName()}</td>
        <td>${contact.lastName()}</td>
        <td>${contact.phone()}</td>
        <td>${contact.email()}</td>
        <td><a href="/contacts/${contact.id()}/edit">Edit</a><a href="/contacts/${contact.id()}">View</a></td>
      </tr>
    @endfor
    </tbody>
  </table>

  <p><a href="/contacts/new">Add contact</a></p>
`)
