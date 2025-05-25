import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatDividerModule
  ],
  template: `
    <mat-toolbar class="header-toolbar">
      <div class="header-content">
        <div class="logo-section">
          <mat-icon class="logo-icon">analytics</mat-icon>
          <span class="app-title">QuickBooks Sales Analytics</span>
        </div>

        <div class="user-section">
          <span class="user-info">Welcome, Walter Chen</span>
          <button mat-icon-button [matMenuTriggerFor]="userMenu" class="user-avatar">
            <mat-icon>wc</mat-icon>
          </button>

          <mat-menu #userMenu="matMenu">
            <button mat-menu-item>
              <mat-icon>person</mat-icon>
              <span>Profile</span>
            </button>
            <button mat-menu-item>
              <mat-icon>settings</mat-icon>
              <span>Settings</span>
            </button>
            <mat-divider></mat-divider>
            <button mat-menu-item>
              <mat-icon>logout</mat-icon>
              <span>Logout</span>
            </button>
          </mat-menu>
        </div>
      </div>
    </mat-toolbar>
  `,
  styles: [`
    .header-toolbar {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      box-shadow: 0 2px 10px rgba(0,0,0,0.1);
      position: sticky;
      top: 0;
      z-index: 1000;
    }

    .header-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
      width: 100%;
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 16px;
    }

    .logo-section {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .logo-icon {
      font-size: 32px;
      width: 32px;
      height: 32px;
    }

    .app-title {
      font-size: 24px;
      font-weight: 600;
      letter-spacing: 0.5px;
    }

    .user-section {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .user-info {
      font-size: 14px;
      opacity: 0.9;
    }

    .user-avatar {
      color: white;
    }

    .user-avatar mat-icon {
      font-size: 32px;
      width: 32px;
      height: 32px;
    }

    @media (max-width: 768px) {
      .user-info {
        display: none;
      }

      .app-title {
        font-size: 18px;
      }
    }
  `]
})
export class HeaderComponent {
}
