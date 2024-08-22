@import hr.tjakopan.contactapp.ArchiverState
@import hr.tjakopan.contactapp.ArchiveStatus

@param ArchiverState archiverState

<div id="archive-ui" hx-target="this" hx-swap="outerHTML">
  @if(archiverState.status() == ArchiveStatus.WAITING)
    <button hx-post="/contacts/archive">Download contacts archive</button>
  @elseif(archiverState.status() == ArchiveStatus.RUNNING)
    <div hx-get="/contacts/archive" hx-trigger="load delay:500ms">
      Creating archive...
      <div class="progress">
        <div id="archive-progress" class="progress-bar" role="progressbar"
             aria-valuenow="${archiverState.progress() * 100}"
             style="width: ${archiverState.progress() * 100}%"></div>
      </div>
    </div>
  @elseif(archiverState.status() == ArchiveStatus.COMPLETE)
    <a hx-boost="false" href="/contacts/archive/file" _="on load click() me">
      Archive downloading! Click here if the download does not start.
    </a>
    <button hx-delete="/contacts/archive">Clear download</button>
  @endif
</div>
