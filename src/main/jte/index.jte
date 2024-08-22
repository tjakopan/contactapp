@import java.util.List
@import hr.tjakopan.contactapp.ValidContact
@import hr.tjakopan.contactapp.ArchiverState

@param String q = null
@param List<ValidContact> contacts
@param String flashMessage = null
@param int count = 0
@param ArchiverState archiverState

@template.layout(flashMessage = flashMessage, content = @`
  @template.archive_ui(archiverState = archiverState)

  <form action="/contacts" method="get" class="tool-bar">
    <label for="search">Search term</label>
    <input id="search" type="search" name="q" value="${q}" hx-get="/contacts"
           hx-trigger="search, keyup delay:200ms change" hx-target="tbody" hx-push-url="true" hx-indicator="#spinner"
           _="on keydown[altKey and code is 'KeyS'] from the window focus() me"/>
    <img id="spinner" class="htmx-indicator" style="height: 20px" src="/static/img/spinning-circles.svg"
         alt="Request in flight..."/>
    <input type="submit" value="Search"/>
  </form>

  <form x-data="{ selected: [] }">
    <template x-if="selected.length > 0">
      <div class="box info tool-bar">
        <slot x-text="selected.length"></slot>
        contacts selected

        @raw
        <button type="button" class="bad bg color border"
                @click="confirm(`Delete ${selected.length} contacts?`) && htmx.ajax('DELETE', '/contacts', {source: $root, target: document.body})">
          Delete
        </button>
        @endraw
        <hr aria-orientation="vertical"/>
        <button type="button" @click="selected = []">Cancel</button>
      </div>
    </template>
    <table>
      <thead>
      <tr>
        <th></th>
        <th>First</th>
        <th>Last</th>
        <th>Phone</th>
        <th>Email</th>
        <th></th>
      </tr>
      </thead>
      <tbody>
      @template.rows(contacts = contacts)
      </tbody>
    </table>
    <button hx-delete="/contacts" hx-confirm="Are you sure you want to delete these contacts?" hx-target="body">
      Delete selected contacts
    </button>
  </form>
  <p>
    <a href="/contacts/new">Add contact</a>
    <span hx-get="/contacts/count" hx-trigger="revealed">
      <img id="spinner" class="htmx-indicator" style="height: 20px" src="/static/img/spinning-circles.svg"
           alt="Request in flight..."/>
    </span>
  </p>
`)
