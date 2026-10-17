import { Link } from 'react-router-dom'

export function Portfolio() {
  return (
    <div className="page portfolio">
      <header className="hero">
        <p className="eyebrow">Proof of concept · October 2026</p>
        <h1>Dependency Migration Intelligence</h1>
        <p className="lede">
          Explain a single Maven dependency upgrade with traceable documentation citations and bounded
          static source evidence — without claiming an unexecuted migration is safe.
        </p>
        <div className="hero-actions">
          <Link className="button primary" to="/analyze">
            Run the demo analysis
          </Link>
          <a
            className="button ghost"
            href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide"
            target="_blank"
            rel="noreferrer"
          >
            Spring Boot 3 migration guide
          </a>
        </div>
      </header>

      <section className="grid three">
        <article>
          <h2>Problem</h2>
          <p>
            Java teams upgrading Spring Boot must connect release notes to their own code and separate
            verified breakage from plausible risks.
          </p>
        </article>
        <article>
          <h2>MVP scope</h2>
          <ul>
            <li>One allowlisted fixture (<code>spring-migration-case</code>)</li>
            <li>Parent bump 2.7.18 → 3.2.12</li>
            <li>Deterministic scanner + curated catalogue</li>
            <li>No Maven execution, no LLM by default</li>
          </ul>
        </article>
        <article>
          <h2>Architecture</h2>
          <p>
            React UI → Spring Boot API → fixture snapshot, POM reader, pattern scanner, evidence classifier →
            structured report with explicit limitations.
          </p>
        </article>
      </section>

      <section className="pipeline">
        <h2>Analysis pipeline</h2>
        <ol>
          <li>Validate request boundaries (size, tokens, supported migration)</li>
          <li>Load immutable fixture and digest contents</li>
          <li>Read declared parent coordinates from <code>pom.xml</code></li>
          <li>Scan allowlisted sources with hard caps</li>
          <li>Match curated upgrade rules and classify confidence</li>
          <li>Compose report — never invent citations</li>
        </ol>
      </section>

      <section className="status">
        <h2>Validation status</h2>
        <p>
          <strong>SPIKE / MVP:</strong> Backend integration tests cover golden findings, negative paths, and
          path traversal rejection. OpenRewrite baseline comparison is documented under{' '}
          <code>docs/baseline/</code> for the next gate.
        </p>
      </section>
    </div>
  )
}
