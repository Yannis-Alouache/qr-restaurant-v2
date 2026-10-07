import { Component } from '@angular/core';

/**
 * Écran affiché pour toute URL hors du plan de routage (wildcard « ** » des
 * app.routes) ainsi que pour la racine « / » : le parcours n'a pas d'écran
 * d'accueil, l'entrée se fait par le lien du QR code de table. Sans action,
 * comme l'écran QR invalide : la seule issue utile est de scanner à nouveau
 * le QR code de sa table. L'URL ne porte pas de slug : le thème affiché est
 * celui du dernier restaurant visité, restauré au démarrage par le
 * ThemeService.
 */
@Component({
  selector: 'app-not-found-screen',
  imports: [],
  templateUrl: './not-found-screen.component.html',
  styleUrl: './not-found-screen.component.scss',
})
export class NotFoundScreenComponent {}
