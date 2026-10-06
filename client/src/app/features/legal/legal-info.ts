/**
 * Identité légale de l'éditeur du service — source unique consommée par les
 * pages /legal/mentions-legales, /legal/confidentialite et /legal/cgv.
 *
 * ⚠️ Miroir de admin/src/app/features/legal/legal-info.ts : toute modification
 * doit être répercutée des deux côtés tant que le code n'est pas mutualisé.
 *
 * TODO(légal): chaque valeur marquée `[À COMPLÉTER]` DOIT être remplacée par
 * l'information réelle avant l'ouverture au public — l'affichage des mentions
 * de l'éditeur et de l'hébergeur est une obligation de la LCEN (art. 6-III).
 * Suivi : docs/deploiement/checklist-go-live.md, section « Légal & conformité ».
 */
export const LEGAL_INFO = {
  /** Marque commerciale affichée sur l'interface. */
  brand: 'Menzo',

  // ── Éditeur (LCEN art. 6-III) ──────────────────────────────────────────
  /** Raison sociale complète, ex. « Menzo SAS ». */
  editorName: '[À COMPLÉTER : raison sociale, ex. Menzo SAS]',
  legalForm: '[À COMPLÉTER : forme juridique — SAS, EURL, EI…]',
  /** Ville du greffe + numéro, ex. « RCS Paris B 123 456 789 ». */
  rcs: '[À COMPLÉTER : ville + n° RCS ou n° SIREN]',
  headOffice: '[À COMPLÉTER : adresse du siège social]',
  contactEmail: '[À COMPLÉTER : email de contact]',
  publicationDirector: '[À COMPLÉTER : nom du directeur de la publication]',

  // ── Hébergeur (LCEN art. 6-I 2°) ───────────────────────────────────────
  /** Prestataire du VPS qui fait tourner Coolify, ex. « Hetzner Online GmbH ». */
  hostName: '[À COMPLÉTER : nom de l’hébergeur du VPS]',
  hostLegalForm: '[À COMPLÉTER : forme juridique et capital de l’hébergeur]',
  hostAddress: '[À COMPLÉTER : adresse de l’hébergeur]',
  hostPhone: '[À COMPLÉTER : téléphone de l’hébergeur]',

  // ── RGPD ───────────────────────────────────────────────────────────────
  /** Contact déclaré dans la politique de confidentialité (DPO ou référent). */
  dpoEmail: '[À COMPLÉTER : email contact RGPD]',
} as const;

export type LegalInfo = typeof LEGAL_INFO;

/** Vrai tant qu'au moins un champ légal reste à renseigner. */
export const LEGAL_PENDING = Object.values(LEGAL_INFO).some((value) =>
  value.includes('[À COMPLÉTER'),
);
