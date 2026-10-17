export type Confidence = 'CONFIRMED_STATIC' | 'POSSIBLE' | 'NOT_ASSESSED'

export type MigrationVerdict =
  | 'ACTION_REQUIRED'
  | 'REVIEW_RECOMMENDED'
  | 'INFORMATIONAL'
  | 'FALSE_POSITIVE'
  | 'UNVERIFIED'

export type OpenRewriteComparisonStatus =
  | 'PATCHED_IN_BASELINE'
  | 'NOT_PRESENT_IN_PATCH'
  | 'NOT_TESTED'

export interface MigrationEvidenceRef {
  url: string
  title: string
  section: string
}

export interface MigrationReview {
  verdict: MigrationVerdict
  openRewriteComparison: OpenRewriteComparisonStatus
  evidenceProves: string
  evidence: MigrationEvidenceRef[]
  testedScenario: string
  openRewriteRecipe: string
  openRewritePluginVersion: string
  openRewriteRecipeArtifactVersion: string
  reviewerNotes: string
  lastReviewedUtc: string
  nextHumanCheck: string
}

export interface SourceLocation {
  path: string
  line: number
}

export interface DocumentationRef {
  url: string
  title: string
  section: string
}

export interface Finding {
  id: string
  category: string
  confidence: Confidence
  summary: string
  sourceLocations: SourceLocation[]
  documentation: DocumentationRef[]
  rationale: string
  proposedChecks: string[]
  migrationReview?: MigrationReview
}

export interface AnalysisReport {
  requestId: string
  fixtureDigest: string
  generatedAtUtc: string
  before: { group: string; artifact: string; version: string }
  after: { group: string; artifact: string; version: string }
  findings: Finding[]
  limitations: string[]
  baselineComparisonRef: string
}

export interface AnalysisRequest {
  fixtureId: string
  dependencyGroup: string
  dependencyArtifact: string
  fromVersion: string
  toVersion: string
}

export interface ApiError {
  code: string
  message: string
  traceId: string
  details: string[]
}

export type ExplanationStatus =
  | 'ACCEPTED'
  | 'REJECTED'
  | 'DISABLED'
  | 'FALLBACK'
  | 'TIMEOUT'

export interface ExplanationClaim {
  text: string
  evidenceIds: string[]
}

export interface ExplanationModelMetadata {
  provider: string
  modelId: string
  aiAssisted: boolean
  experimentStatus: string
}

export interface FindingExplanation {
  findingId: string
  evidencePackId: string
  status: ExplanationStatus
  summary: string
  claims: ExplanationClaim[]
  nextChecks: string[]
  limitations: string[]
  modelMetadata: ExplanationModelMetadata
  deterministicReview?: MigrationReview
}

export interface ExplanationsResponse {
  report: AnalysisReport
  explanations: FindingExplanation[]
}

export interface ExplanationRequest {
  analysis: AnalysisRequest
  findingIds: string[]
}
