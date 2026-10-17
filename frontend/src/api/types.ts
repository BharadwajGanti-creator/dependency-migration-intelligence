export type Confidence = 'CONFIRMED_STATIC' | 'POSSIBLE' | 'NOT_ASSESSED'

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
