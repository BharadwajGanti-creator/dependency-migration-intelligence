import type { ExplanationStatus, Finding, FindingExplanation, MigrationVerdict } from '../api/types'

const confidenceLabel: Record<Finding['confidence'], string> = {
  CONFIRMED_STATIC: 'Confirmed (static)',
  POSSIBLE: 'Possible',
  NOT_ASSESSED: 'Not assessed',
}

const explanationStatusLabel: Record<ExplanationStatus, string> = {
  ACCEPTED: 'AI-assisted explanation (accepted)',
  REJECTED: 'AI explanation rejected',
  DISABLED: 'AI explanations disabled',
  FALLBACK: 'Deterministic fallback',
  TIMEOUT: 'Explanation timed out',
}

const verdictLabel: Record<MigrationVerdict, string> = {
  ACTION_REQUIRED: 'Reviewed: action required',
  REVIEW_RECOMMENDED: 'Reviewed: manual review',
  INFORMATIONAL: 'Reviewed: informational',
  FALSE_POSITIVE: 'Reviewed: false positive',
  UNVERIFIED: 'Reviewed: unverified',
}

export function FindingList({
  findings,
  explanationsByFindingId = {},
  onRequestExplanation,
  explanationLoadingId,
  explanationError,
}: {
  findings: Finding[]
  explanationsByFindingId?: Record<string, FindingExplanation>
  onRequestExplanation?: (findingId: string) => void
  explanationLoadingId?: string | null
  explanationError?: string | null
}) {
  if (findings.length === 0) {
    return <p className="muted">No findings returned.</p>
  }

  return (
    <ul className="finding-list">
      {findings.map((f) => (
        <li key={f.id} className={`finding-card confidence-${f.confidence}`}>
          <header>
            <span className="badge">{confidenceLabel[f.confidence]}</span>
            {f.migrationReview && (
              <span className={`badge verdict verdict-${f.migrationReview.verdict}`}>
                {verdictLabel[f.migrationReview.verdict]}
              </span>
            )}
            <h3>{f.id}</h3>
          </header>
          <p>{f.summary}</p>
          {f.migrationReview && (
            <div className="migration-review">
              <h4>Reviewed migration consequence</h4>
              <p className="review-note">
                Static detection ({confidenceLabel[f.confidence]}) is separate from the reviewed verdict
                below. Neither implies runtime verification or merge safety.
              </p>
              <p>{f.migrationReview.evidenceProves}</p>
              <dl className="review-meta">
                <div>
                  <dt>OpenRewrite baseline</dt>
                  <dd>{f.migrationReview.openRewriteComparison.replaceAll('_', ' ').toLowerCase()}</dd>
                </div>
                <div>
                  <dt>Tested scenario</dt>
                  <dd>{f.migrationReview.testedScenario}</dd>
                </div>
                <div>
                  <dt>Next human check</dt>
                  <dd>{f.migrationReview.nextHumanCheck}</dd>
                </div>
                {f.migrationReview.openRewriteRecipe && (
                  <div>
                    <dt>Recipe (offline baseline)</dt>
                    <dd>
                      <code>{f.migrationReview.openRewriteRecipe}</code> (plugin{' '}
                      {f.migrationReview.openRewritePluginVersion}, rewrite-spring{' '}
                      {f.migrationReview.openRewriteRecipeArtifactVersion})
                    </dd>
                  </div>
                )}
              </dl>
              {f.migrationReview.evidence.length > 0 && (
                <div>
                  <h5>Review evidence</h5>
                  <ul>
                    {f.migrationReview.evidence.map((d) => (
                      <li key={d.url}>
                        <a href={d.url} target="_blank" rel="noreferrer">
                          {d.title} — {d.section}
                        </a>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
              {f.migrationReview.verdict === 'UNVERIFIED' && (
                <p className="unverified-banner">No reviewed adjudication — do not treat as safe or complete.</p>
              )}
              <p className="rationale muted">{f.migrationReview.reviewerNotes}</p>
            </div>
          )}
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

          <div className="explanation-section" aria-live="polite">
            <h4>Optional AI-assisted explanation</h4>
            <p className="review-note muted">
              Opt-in only. Reviewed B2 facts above remain authoritative; explanations never imply runtime
              verification or merge safety.
            </p>
            {onRequestExplanation && (
              <button
                type="button"
                className="button secondary explanation-btn"
                disabled={!!explanationLoadingId}
                onClick={() => onRequestExplanation(f.id)}
              >
                {explanationLoadingId === f.id ? 'Requesting…' : 'Request explanation'}
              </button>
            )}
            {explanationError && explanationLoadingId === null && !explanationsByFindingId[f.id] && (
              <p className="alert error" role="alert">
                {explanationError}
              </p>
            )}
            {explanationsByFindingId[f.id] && (
              <div className={`explanation-panel status-${explanationsByFindingId[f.id].status}`}>
                <span className="badge explanation-status">
                  {explanationStatusLabel[explanationsByFindingId[f.id].status]}
                </span>
                <p>{explanationsByFindingId[f.id].summary}</p>
                {explanationsByFindingId[f.id].claims.length > 0 && (
                  <ul>
                    {explanationsByFindingId[f.id].claims.map((c) => (
                      <li key={c.text}>
                        {c.text}{' '}
                        <span className="muted">(evidence: {c.evidenceIds.join(', ')})</span>
                      </li>
                    ))}
                  </ul>
                )}
                <p className="muted">
                  Provider: {explanationsByFindingId[f.id].modelMetadata.provider} · experiment:{' '}
                  {explanationsByFindingId[f.id].modelMetadata.experimentStatus}
                </p>
              </div>
            )}
          </div>
        </li>
      ))}
    </ul>
  )
}
