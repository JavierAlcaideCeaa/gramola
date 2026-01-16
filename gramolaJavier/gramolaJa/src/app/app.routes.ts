import { Routes } from '@angular/router';
import { Register } from './register/register';
import { Login } from './login/login';
import { CallbackComponent } from './callback/callback';
import { Confirm } from './confirm/confirm';
import { PaymentComponent } from './payment/payment';
import { DashboardComponent } from './dashboard/dashboard';
import { ForgotPasswordComponent } from './forgot-password/forgot-password';
import { ResetPasswordComponent } from './reset-password/reset-password';

export const routes: Routes = [
    { path: '', redirectTo: '/login', pathMatch: 'full' },
    { path: 'register', component: Register },
    { path: 'login', component: Login },
    { path: 'confirm', component: Confirm }, 
    { path: 'callback', component: CallbackComponent },
    { path: 'payment', component: PaymentComponent },
    { path: 'dashboard', component: DashboardComponent },
    { path: 'forgot-password', component: ForgotPasswordComponent },
    { path: 'reset-password', component: ResetPasswordComponent }
];