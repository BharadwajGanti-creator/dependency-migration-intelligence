import type { Finding } from '../api/types'

const confidenceLabel: Record<Finding['confidence'], string> = {
  CONFIRMED_STATIC: 'Confirmed (static)',
  POSSIBLE: 'Possible',
  NOT_ASSESSED: 'Not assessed',
}

export function FindingList({ findings }: { findings: Finding[] }) {
  if (findings.length === 0) {
    return <p className="muted">No findings returned.</p>
  }

  return (
    <ul className="finding-list">
      {findings.map((f) => (
        <li key={f.id} className={`finding-card confidence-${f.confidence}`}>
          <header>
            <span className="badge">{confidenceLabel[f.confidence]}</span>
            <h3>{f.id}</h3>
          </header>
          <p>{f.summary}</p>
          {f.sourceLocations.length > 0 && (
            <div>
              <h4>Source evidence</h4>
              <ul className="evidence">
                {f.sourceLocations.map((loc) => (
                  <li key={`${loc.path}:${loc.line}`}>
                    <code>
                      {loc.path}:{loc.line}
                    </code>
                  </li>
                ))}
              </ul>
            </div>
          )}
          {f.documentation.length > 0 && (
            <div>
              <h4>Documentation</h4>
              <ul>
                {f.documentation.map((d) => (
                  <li key={d.url}>
                    <a href={d.url} target="_blank" rel="noreferrer">
                      {d.title} — {d.section}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          )}
          {f.proposedChecks.length > 0 && (
            <div>
              <h4>Next checks</h4>
              <ul>
                {f.proposedChecks.map((c) => (
                  <li key={c}>{c}</li>
                ))}
              </ul>
            </div>
          )}
          <p className="rationale">{f.rationale}</p>
        </li>
      ))}
    </ul>
  )
}
