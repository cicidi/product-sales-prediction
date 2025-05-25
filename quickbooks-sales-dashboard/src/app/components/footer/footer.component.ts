import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule
  ],
  template: `
    <footer class="app-footer">
      <div class="footer-content">
        <div class="footer-section">
          <div class="company-info">
            <h3>QuickBooks Sales Analytics</h3>
            <p>Advanced sales prediction and analytics platform</p>
          </div>
        </div>
        
        <div class="footer-section">
          <div class="author-info">
            <h4>Developed by</h4>
            <p><strong>Walter Chen</strong></p>
            <p>Professional Software Solutions</p>
          </div>
        </div>
        
        <div class="footer-section">
          <div class="contact-info">
            <h4>Connect with us</h4>
            <div class="social-links">
              <button mat-icon-button>
                <mat-icon>email</mat-icon>
              </button>
              <button mat-icon-button>
                <mat-icon>language</mat-icon>
              </button>
              <button mat-icon-button>
                <mat-icon>code</mat-icon>
              </button>
            </div>
          </div>
        </div>
      </div>
      
      <div class="footer-bottom">
        <div class="copyright">
          <p>&copy; {{ currentYear }} QuickBooks Sales Analytics. All rights reserved.</p>
          <p class="tech-stack">Built with Angular {{ angularVersion }} • Material Design • Chart.js</p>
        </div>
      </div>
    </footer>
  `,
  styles: [`
    .app-footer {
      background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
      color: white;
      margin-top: auto;
    }

    .footer-content {
      max-width: 1200px;
      margin: 0 auto;
      padding: 20px 16px 10px;
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 30px;
    }

    .footer-section h3 {
      color: #3498db;
      margin-bottom: 10px;
      font-size: 20px;
      font-weight: 600;
    }

    .footer-section h4 {
      color: #ecf0f1;
      margin-bottom: 15px;
      font-size: 16px;
      font-weight: 500;
    }

    .footer-section p {
      color: #bdc3c7;
      line-height: 1.6;
      margin-bottom: 8px;
    }

    .company-info p {
      font-size: 14px;
      margin-top: 10px;
    }

    .author-info strong {
      color: #e74c3c;
      font-weight: 600;
    }

    .social-links {
      display: flex;
      gap: 8px;
      margin-top: 10px;
    }

    .social-links button {
      color: #bdc3c7;
      transition: color 0.3s ease;
    }

    .social-links button:hover {
      color: #3498db;
    }

    .footer-bottom {
      border-top: 1px solid #34495e;
      padding: 10px 16px;
      text-align: center;
    }

    .copyright {
      max-width: 1200px;
      margin: 0 auto;
    }

    .copyright p {
      color: #95a5a6;
      font-size: 14px;
      margin-bottom: 5px;
    }

    .tech-stack {
      font-size: 12px !important;
      opacity: 0.8;
    }

    @media (max-width: 768px) {
      .footer-content {
        grid-template-columns: 1fr;
        text-align: center;
        padding: 30px 16px 15px;
      }
      
      .social-links {
        justify-content: center;
      }
    }
  `]
})
export class FooterComponent {
  currentYear = new Date().getFullYear();
  angularVersion = '17';
} 