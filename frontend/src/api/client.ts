import type { AnalysisReport, AnalysisRequest, ApiError } from './types'

const API_BASE = import.meta.env.VITE_API_BASE ?? ''

export async function runAnalysis(body: AnalysisRequest): Promise<AnalysisReport> {
  const res = await fetch(`${API_BASE}/api/v1/analyses/sync`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  const payload = await res.json()
  if (!res.ok) {
    const err = payload as ApiError
    throw new Error(`${err.code}: ${err.message}`)
  }
  return payload as AnalysisReport
}
