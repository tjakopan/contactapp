@import java.util.Map
@import hr.tjakopan.contactapp.Contact

@param Contact contact
@param Map<String, String> errors = java.util.Map.<String, String>of()


@template.layout(content = @`
  <form action="/contacts/${contact.id()}/edit" method="post">
    <fieldset>
      <legend>Contact values</legend>
      <p>
        <label for="email">Email</label>
        <input name="email" id="email" type="text" placeholder="Email" value="${contact.email()}"
               hx-get="/contacts/${contact.id()}/email" hx-target="next .error"
               hx-trigger="change, keyup delay:200ms changed">
        <span class="error">${errors.get("email")}</span>
      </p>
      <p>
        <label for="first_name">First name</label>
        <input name="first_name" id="first_name" type="text" placeholder="First name" value="${contact.firstName()}">
        <span class="error">${errors.get("firstName")}</span>
      </p>
      <p>
        <label for="last_name">Last name</label>
        <input name="last_name" id="last_name" type="text" placeholder="Last name" value="${contact.lastName()}">
        <span class="error">${errors.get("lastName")}</span>
      </p>
      <p>
        <label for="phone">Phone</label>
        <input name="phone" id="phone" type="text" placeholder="Phone" value="${contact.phone()}">
        <span class="error">${errors.get("phone")}</span>
      </p>
      <button>Save</button>
    </fieldset>
  </form>
  <button id="delete-btn" hx-delete="/contacts/${contact.id()}" hx-target="body" hx-push-url="true"
          hx-confirm="Are you sure you want to delete this contact?">Delete contact
  </button>
  <p>
    <a href="/contacts/">Back</a>
  </p>
`)
